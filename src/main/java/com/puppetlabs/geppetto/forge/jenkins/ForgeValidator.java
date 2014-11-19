/**
 * Copyright (c) 2014 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.forge.jenkins;

import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.FORGE_SERVICE_URL;
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkExcludeGlobs;
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkRelativePath;
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkURL;
import static java.lang.String.format;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Maps;
import com.puppetlabs.geppetto.common.Strings;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;
import com.puppetlabs.geppetto.forge.jenkins.ModuleValidationAdvisor.ModuleValidationAdvisorDescriptor;
import com.puppetlabs.geppetto.forge.jenkins.PPProblemsAdvisor.ProblemsAdvisorDescriptor;
import com.puppetlabs.geppetto.forge.model.ForgeDocs;
import com.puppetlabs.geppetto.forge.model.ForgeResult;
import com.puppetlabs.geppetto.forge.model.VersionedName;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner;
import com.puppetlabs.geppetto.validation.ValidationOptions;

public class ForgeValidator extends Builder {

	@Extension
	public static final class ForgeValidatorDescriptor extends BuildStepDescriptor<Builder> {
		public ForgeValidatorDescriptor() {
			super(ForgeValidator.class);
		}

		public FormValidation doCheckExcludeGlobs(@QueryParameter String value) throws IOException, ServletException {
			return checkExcludeGlobs(value);
		}

		public FormValidation doCheckForgeServiceURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		public FormValidation doCheckJsonResultPath(@QueryParameter String value) throws IOException, ServletException {
			return checkRelativePath(value);
		}

		public FormValidation doCheckJsonTypesPath(@QueryParameter String value) throws IOException, ServletException {
			return checkRelativePath(value);
		}

		public FormValidation doCheckPuppetLintOptions(@QueryParameter String value) throws IOException, ServletException {
			try {
				parsePuppetLintOptions(value, new boolean[1]);
				return FormValidation.ok();
			}
			catch(FormValidation e) {
				return e;
			}
		}

		public FormValidation doCheckSourcePath(@QueryParameter String value) throws IOException, ServletException {
			return checkRelativePath(value);
		}

		public ListBoxModel doFillComplianceLevelItems() {
			return fillEnumItems(ComplianceLevel.values());
		}

		public ListBoxModel doFillMaxComplianceLevelItems() {
			return doFillComplianceLevelItems();
		}

		public ListBoxModel doFillPuppetLintMaxSeverityItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillValidationImpactItems() {
			return fillEnumItems(ValidationImpact.values());
		}

		private <T extends Enum<?>> ListBoxModel fillEnumItems(T[] values) {
			ListBoxModel items = new ListBoxModel();
			for(T value : values)
				items.add(value.toString(), value.name());
			return items;
		}

		/**
		 * @return the default Puppet Compliance Level
		 */
		public String getComplianceLevel() {
			return PuppetTarget.getDefault().getComplianceLevel().name();
		}

		public String getDefaultExcludeGlobs() {
			return ForgeBuilder.joinExcludeGlobs(ValidationOptions.DEFAULT_EXCLUDES);
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return "Validate Puppet Module";
		}

		/**
		 * @return the default Forge Service URL
		 */
		public String getForgeServiceURL() {
			return FORGE_SERVICE_URL;
		}

		public Descriptor<ModuleValidationAdvisor> getModuleValidationAdvisorDescriptor() {
			return Jenkins.getInstance().getDescriptorByType(ModuleValidationAdvisorDescriptor.class);
		}

		public Descriptor<PPProblemsAdvisor> getProblemsAdvisorDescriptor() {
			return Jenkins.getInstance().getDescriptorByType(ProblemsAdvisorDescriptor.class);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	public static class VersionedNameSerializer extends JsonSerializer<VersionedName> {
		@Override
		public void serialize(VersionedName value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
				JsonProcessingException {
			StringBuilder bld = new StringBuilder();
			value.toString(bld, '-');
			jgen.writeString(bld.toString());
		}
	}

	public static DiagnosticType VALIDATOR_TYPE = new DiagnosticType("VALIDATOR", ForgeValidator.class.getName());

	public static ListBoxModel doFillValidationPreferenceItems() {
		List<hudson.util.ListBoxModel.Option> items = new ArrayList<hudson.util.ListBoxModel.Option>();
		for(ValidationPreference pref : ValidationPreference.values())
			items.add(new hudson.util.ListBoxModel.Option(pref.toString(), pref.name()));
		return new ListBoxModel(items);
	}

	public static ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule("Geppetto Custom Serializers");
		VersionedNameSerializer vnSerializer = new VersionedNameSerializer();
		module.addKeySerializer(VersionedName.class, vnSerializer);
		module.addSerializer(VersionedName.class, vnSerializer);
		mapper.registerModule(module);
		return mapper;
	}

	static String[] parsePuppetLintOptions(String puppetLintOptions, boolean[] inverted) throws FormValidation {
		if(puppetLintOptions != null) {
			puppetLintOptions = puppetLintOptions.trim();
			if(puppetLintOptions.length() == 0)
				puppetLintOptions = null;
		}
		if(puppetLintOptions == null)
			return new String[0];

		FormValidation validationResult = FormValidation.error("Invalid Puppet Lint Option");
		boolean isInverted = puppetLintOptions.startsWith("-");
		int start = 0;
		if(isInverted)
			++start;

		boolean allValid = true;
		ArrayList<String> optionList = new ArrayList<String>();
		int top = puppetLintOptions.length();
		int commaIdx;
		while(start < top && (commaIdx = puppetLintOptions.indexOf(',', start)) > 0) {
			try {
				optionList.add(validatePuppetLintOption(puppetLintOptions.substring(start, commaIdx)));
			}
			catch(IllegalArgumentException e) {
				allValid = false;
				validationResult.addSuppressed(e);
			}
			start = commaIdx + 1;
		}
		if(start < top)
			try {
				optionList.add(validatePuppetLintOption(puppetLintOptions.substring(start)));
			}
			catch(IllegalArgumentException e) {
				allValid = false;
				validationResult.addSuppressed(e);
			}

		if(!allValid)
			throw validationResult;

		inverted[0] = isInverted;
		return optionList.toArray(new String[optionList.size()]);
	}

	private static String validatePuppetLintOption(String check) throws IllegalArgumentException {
		if(!PuppetLintRunner.CHECK_PATTERN.matcher(check).matches() || PuppetLintRunner.NOT_CHECK_OPTIONS.contains(check))
			throw new IllegalArgumentException('\'' + check + "' is not a valid puppet-lint check");
		return check;
	}

	private final ComplianceLevel complianceLevel;

	private final ComplianceLevel maxComplianceLevel;

	private final boolean checkReferences;

	private final boolean checkModuleSemantics;

	private final boolean ignoreFileOverride;

	private final boolean inverseOptions;

	private final boolean produceGraph;

	private final String jsonDocsPath;

	private final PPProblemsAdvisor problemsAdvisor;

	private final ModuleValidationAdvisor moduleValidationAdvisor;

	private final ValidationPreference puppetLintMaxSeverity;

	private final String[] puppetLintOptions;

	private final String sourcePath;

	private final String jsonResultPath;

	private final String forgeServiceURL;

	private final Set<String> excludeGlobs;

	private final ValidationImpact validationImpact;

	private transient ForgeValidatorCallable callable;

	@DataBoundConstructor
	public ForgeValidator(String sourcePath, String jsonResultPath, String jsonDocsPath, String forgeServiceURL,
			boolean ignoreFileOverride, String excludes, ComplianceLevel complianceLevel, ComplianceLevel maxComplianceLevel,
			Boolean checkReferences, Boolean checkModuleSemantics, PPProblemsAdvisor problemsAdvisor,
			ModuleValidationAdvisor moduleValidationAdvisor, ValidationPreference puppetLintMaxSeverity, String puppetLintOptions,
			ValidationImpact validationImpact, boolean produceGraph) throws FormValidation {
		this.sourcePath = sourcePath;
		this.jsonResultPath = jsonResultPath;
		this.forgeServiceURL = forgeServiceURL == null
			? FORGE_SERVICE_URL
			: forgeServiceURL;
		this.ignoreFileOverride = ignoreFileOverride;
		this.excludeGlobs = ForgeBuilder.parseExcludeGlobs(excludes);
		this.complianceLevel = complianceLevel;
		this.maxComplianceLevel = maxComplianceLevel;
		this.checkReferences = checkReferences == null
			? false
			: checkReferences.booleanValue();
		this.checkModuleSemantics = checkModuleSemantics == null
			? false
			: checkModuleSemantics.booleanValue();
		this.problemsAdvisor = problemsAdvisor;
		this.moduleValidationAdvisor = moduleValidationAdvisor;
		this.puppetLintMaxSeverity = puppetLintMaxSeverity == null
			? ValidationPreference.IGNORE
			: puppetLintMaxSeverity;
		boolean[] inverse = new boolean[1];
		this.puppetLintOptions = parsePuppetLintOptions(puppetLintOptions, inverse);
		inverseOptions = inverse[0];
		this.jsonDocsPath = jsonDocsPath;
		this.validationImpact = validationImpact;
		this.produceGraph = produceGraph;
	}

	private ComplianceLevel _maxComplianceLevel() {
		return maxComplianceLevel == null
			? _minComplianceLevel()
			: maxComplianceLevel;
	}

	private ComplianceLevel _minComplianceLevel() {
		return complianceLevel == null
			? PuppetTarget.getDefault().getComplianceLevel()
			: complianceLevel;
	}

	private ValidationImpact _validationImpact() {
		return validationImpact == null
			? ValidationImpact.DO_NOT_FAIL
			: validationImpact;
	}

	private boolean consoleReport(Diagnostic diag, BuildListener listener, boolean[] hardError) {
		if(diag instanceof ValidationDiagnostic)
			return diag.getSeverity() == Diagnostic.ERROR;

		List<Diagnostic> children = diag.getChildren();
		if(!children.isEmpty()) {
			boolean validationErrors = false;
			for(Diagnostic cdiag : diag)
				if(consoleReport(cdiag, listener, hardError))
					validationErrors = true;
			return validationErrors;
		}
		if(diag instanceof ComplianceDiagnostic)
			// Empty compliance diagnostic. Don't report the compliance level
			return false;

		switch(diag.getSeverity()) {
			case Diagnostic.ERROR:
				listener.error("%s", diag);
				hardError[0] = true;
				break;
			case Diagnostic.FATAL:
				listener.fatalError("%s", diag);
				hardError[0] = true;
				break;
			default:
				listener.getLogger().format("%s%n", diag);
		}
		return false;
	}

	private ForgeResult createForgeResult(ValidationResult data, VersionedName moduleSlug, Properties props) {
		ForgeResult forgeResult = new ForgeResult();
		forgeResult.setName(format("%s.%s", props.getProperty("groupId"), props.getProperty("artifactId")));
		forgeResult.setVersion(format("%s-%s", props.getProperty("version"), props.getProperty("git.build.time")));
		forgeResult.setRelease(moduleSlug);
		Map<String, Object> resultMap = new HashMap<>();
		for(Diagnostic diag : data.getUnfilteredLevelDiagnostics()) {
			ComplianceDiagnostic cdiag = (ComplianceDiagnostic) diag;
			Map<String, Object> cdiagMap = Maps.newHashMap();
			cdiagMap.put("severity", cdiag.getSeverityString());
			cdiagMap.put("diagnostics", cdiag.getChildren());
			resultMap.put(cdiag.getComplianceLevel().toString(), cdiagMap);
		}
		forgeResult.setResults(resultMap);
		return forgeResult;
	}

	private synchronized ForgeValidatorCallable getCallable(String sourceURI, String branch) {
		if(callable == null) {
			ComplianceLevel min = _minComplianceLevel();
			ComplianceLevel max = _maxComplianceLevel();
			if(max.ordinal() < min.ordinal()) {
				ComplianceLevel x = min;
				min = max;
				max = x;
			}
			String docsPath = Strings.trimToNull(jsonDocsPath);

			callable = new ForgeValidatorCallable(
				forgeServiceURL, sourceURI, ignoreFileOverride, excludeGlobs, branch, min, max, checkReferences, checkModuleSemantics,
				getProblemsAdvisor().getAdvisor(), getModuleValidationAdvisor().getAdvisor(), puppetLintMaxSeverity, inverseOptions,
				puppetLintOptions, docsPath != null, produceGraph);
		}
		return callable;
	}

	public String getComplianceLevel() {
		return _minComplianceLevel().name();
	}

	public String getExcludeGlobs() {
		return ForgeBuilder.joinExcludeGlobs(excludeGlobs);
	}

	public String getForgeServiceURL() {
		return forgeServiceURL;
	}

	public String getJsonDocsPath() {
		return jsonDocsPath;
	}

	public String getJsonResultPath() {
		return jsonResultPath;
	}

	public String getMaxComplianceLevel() {
		return _maxComplianceLevel().name();
	}

	public ModuleValidationAdvisor getModuleValidationAdvisor() {
		return moduleValidationAdvisor == null
			? new ModuleValidationAdvisor()
			: moduleValidationAdvisor;
	}

	public PPProblemsAdvisor getProblemsAdvisor() {
		return problemsAdvisor == null
			? new PPProblemsAdvisor()
			: problemsAdvisor;
	}

	public String getPuppetLintMaxSeverity() {
		return puppetLintMaxSeverity == null
			? "IGNORE"
			: puppetLintMaxSeverity.name();
	}

	public String getPuppetLintOptions() {
		StringBuilder bld = new StringBuilder();
		if(inverseOptions)
			bld.append('-');
		for(int idx = 0; idx < puppetLintOptions.length; ++idx) {
			if(idx > 0)
				bld.append(',');
			bld.append(puppetLintOptions[idx]);
		}
		return bld.toString();
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public String getValidationImpact() {
		return _validationImpact().name();
	}

	public boolean isCheckModuleSemantics() {
		return checkModuleSemantics;
	}

	public boolean isCheckReferences() {
		return checkReferences;
	}

	public boolean isIgnoreFileOverride() {
		return ignoreFileOverride;
	}

	public boolean isProduceGraph() {
		return produceGraph;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		Properties props = readVersionProperties(listener);
		if(props == null)
			return false;

		listener.getLogger().format(
			"Plug-in: %s.%s.%s-%s%n", props.get("groupId"), props.get("artifactId"), props.get("version"),
			props.get("git.commit.id.abbrev"));

		listener.getLogger().format("Commit time: %s%n", props.get("git.commit.time"));

		listener.getLogger().format("Build time: %s%n", props.get("git.build.time"));

		RepositoryInfo repoInfo = RepositoryInfo.getRepositoryInfo(build, listener);
		FilePath moduleRoot;
		String sourceURI;
		String branch;
		if(repoInfo != null) {
			sourceURI = repoInfo.getRepositoryURL();
			moduleRoot = repoInfo.getGitRoot();
			branch = repoInfo.getBranchName();
		}
		else {
			StringBuilder bld = new StringBuilder(build.getProject().getAbsoluteUrl());
			bld.append("ws/");
			if(sourcePath != null) {
				bld.append(sourcePath);
				if(bld.charAt(bld.length() - 1) != '/')
					bld.append('/');
			}
			sourceURI = bld.toString();
			moduleRoot = build.getWorkspace();
			branch = null;
		}
		String sp = Strings.trimToNull(sourcePath);
		if(sp != null)
			moduleRoot = moduleRoot.child(sp);

		ResultWithDiagnostic<byte[]> result = moduleRoot.act(getCallable(sourceURI, branch));

		// Emit non-validation diagnostics to the console
		boolean[] otherErrors = new boolean[] { false };
		if(consoleReport(result, listener, otherErrors))
			listener.error("There are validation errors. See Validation Result for details");
		if(otherErrors[0])
			return false;

		ValidationResult data = new ValidationResult(build);
		data.setResult(result);
		VersionedName moduleSlug = data.getModuleSlug();

		String jp = Strings.trimToNull(jsonResultPath);
		String dp = Strings.trimToNull(jsonDocsPath);
		if(moduleSlug != null && (jp != null || dp != null)) {
			ObjectMapper mapper = getMapper();
			if(jp != null)
				try (OutputStream out = new BufferedOutputStream(build.getWorkspace().child(jp).write())) {
					mapper.writeValue(out, createForgeResult(data, moduleSlug, props));
				}
			if(dp != null) {
				ForgeDocs docs = result.getExtractedDocs().get(moduleSlug);
				if(docs != null) {
					docs.setName(format("%s.%s", props.getProperty("groupId"), props.getProperty("artifactId")));
					docs.setVersion(format("%s-%s", props.getProperty("version"), props.getProperty("git.build.time")));
					if(docs != null)
						try (OutputStream out = new BufferedOutputStream(build.getWorkspace().child(dp).write())) {
							mapper.writeValue(out, docs);
						}
				}
			}
		}

		// Don't serialize this in the result
		result.setExtractedDocs(Collections.<VersionedName, ForgeDocs> emptyMap());

		build.addAction(data);
		switch(_validationImpact()) {
			case FAIL_ON_ALL:
				return result.getSeverity() < Diagnostic.ERROR;
			case FAIL_ON_ANY:
				return data.getWorstLevelSeverity() < Diagnostic.ERROR;
		}
		return true;
	}

	private Properties readVersionProperties(BuildListener listener) {
		try (InputStream in = getClass().getResourceAsStream("version.properties")) {
			Properties props = new Properties();
			props.load(in);
			return props;
		}
		catch(Exception e) {
			e.printStackTrace(listener.error("The puppetforge plug-in is unable to read its own version.properties"));
			return null;
		}
	}
}
