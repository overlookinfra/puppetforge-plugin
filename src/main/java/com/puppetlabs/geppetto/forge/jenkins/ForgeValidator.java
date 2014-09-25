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
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkRelativePath;
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkURL;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;
import com.puppetlabs.geppetto.forge.jenkins.ModuleValidationAdvisor.ModuleValidationAdvisorDescriptor;
import com.puppetlabs.geppetto.forge.jenkins.PPProblemsAdvisor.ProblemsAdvisorDescriptor;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Option;

public class ForgeValidator extends Builder {

	@Extension
	public static final class ForgeValidatorDescriptor extends BuildStepDescriptor<Builder> {
		public ForgeValidatorDescriptor() {
			super(ForgeValidator.class);
		}

		public FormValidation doCheckForgeServiceURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		public FormValidation doCheckPuppetLintOptions(String puppetLintOptions, FormValidation validationResult) {
			try {
				parsePuppetLintOptions(puppetLintOptions);
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
			ListBoxModel items = new ListBoxModel();
			for(ComplianceLevel level : ComplianceLevel.values())
				items.add(level.toString(), level.name());
			return items;
		}

		public ListBoxModel doFillPuppetLintMaxSeverityItems() {
			return doFillValidationPreferenceItems(ValidationPreference.IGNORE);
		}

		/**
		 * @return the default Puppet Compliance Level
		 */
		public String getComplianceLevel() {
			return PuppetTarget.getDefault().getComplianceLevel().name();
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

	public static ListBoxModel doFillValidationPreferenceItems(ValidationPreference dflt) {
		List<hudson.util.ListBoxModel.Option> items = new ArrayList<hudson.util.ListBoxModel.Option>();
		for(ValidationPreference pref : ValidationPreference.values())
			items.add(new hudson.util.ListBoxModel.Option(pref.toString(), pref.name(), pref == dflt));
		return new ListBoxModel(items);
	}

	private static Option getOption(String s) {
		s = s.trim();
		for(Option option : Option.values()) {
			String on = option.toString();
			// Option string starts with "no-" and ends with "-check". We strip that
			// before comparing.
			if(on.startsWith("no-") && on.substring(3, on.length() - 6).equalsIgnoreCase(s))
				return option;
		}
		throw new IllegalArgumentException("The string '" + s + "' does not represent a known puppet-lint option");
	}

	static Option[] parsePuppetLintOptions(String puppetLintOptions) throws FormValidation {
		if(puppetLintOptions != null) {
			puppetLintOptions = puppetLintOptions.trim();
			if(puppetLintOptions.length() == 0)
				puppetLintOptions = null;
		}
		if(puppetLintOptions == null)
			return new Option[0];

		FormValidation validationResult = FormValidation.error("Invalid Puppet Lint Option");
		boolean isNegative = puppetLintOptions.startsWith("-");
		int start = 0;
		if(isNegative)
			++start;

		boolean allValid = true;
		ArrayList<Option> optionList = new ArrayList<Option>();
		int top = puppetLintOptions.length();
		int commaIdx;
		while(start < top && (commaIdx = puppetLintOptions.indexOf(',', start)) > 0) {
			try {
				optionList.add(getOption(puppetLintOptions.substring(start, commaIdx)));
			}
			catch(IllegalArgumentException e) {
				allValid = false;
				validationResult.addSuppressed(e);
			}
			start = commaIdx + 1;
		}
		if(start < top)
			try {
				optionList.add(getOption(puppetLintOptions.substring(start)));
			}
			catch(IllegalArgumentException e) {
				allValid = false;
				validationResult.addSuppressed(e);
			}

		if(!allValid)
			throw validationResult;

		if(!isNegative) {
			// Inverse the list of options
			ArrayList<Option> inverseList = new ArrayList<Option>();
			for(Option option : Option.values())
				if(!optionList.contains(option))
					inverseList.add(option);
			optionList = inverseList;
		}
		return optionList.toArray(new Option[optionList.size()]);
	}

	public static DiagnosticType VALIDATOR_TYPE = new DiagnosticType("VALIDATOR", ForgeValidator.class.getName());

	private final ComplianceLevel complianceLevel;

	private final boolean checkReferences;

	private final boolean checkModuleSemantics;

	private final PPProblemsAdvisor problemsAdvisor;

	private final ModuleValidationAdvisor moduleValidationAdvisor;

	private final ValidationPreference puppetLintMaxSeverity;

	private final Option[] puppetLintOptions;

	private final String sourcePath;

	private final String forgeServiceURL;

	@DataBoundConstructor
	public ForgeValidator(String sourcePath, String forgeServiceURL, ComplianceLevel complianceLevel, Boolean checkReferences,
			Boolean checkModuleSemantics, PPProblemsAdvisor problemsAdvisor, ModuleValidationAdvisor moduleValidationAdvisor,
			ValidationPreference puppetLintMaxSeverity, String puppetLintOptions) throws FormValidation {
		this.sourcePath = sourcePath;
		this.forgeServiceURL = forgeServiceURL == null
			? FORGE_SERVICE_URL
			: forgeServiceURL;
		this.complianceLevel = complianceLevel == null
			? PuppetTarget.getDefault().getComplianceLevel()
			: complianceLevel;
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
		this.puppetLintOptions = parsePuppetLintOptions(puppetLintOptions);
	}

	public String getComplianceLevel() {
		return complianceLevel.name();
	}

	public String getForgeServiceURL() {
		return forgeServiceURL;
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
		if(puppetLintOptions == null || puppetLintOptions.length == 0)
			return null;
		StringBuilder bld = new StringBuilder(puppetLintOptions[0].toString());
		for(int idx = 1; idx < puppetLintOptions.length; ++idx) {
			bld.append(',');
			bld.append(puppetLintOptions[idx]);
		}
		return bld.toString();
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public boolean isCheckModuleSemantics() {
		return checkModuleSemantics;
	}

	public boolean isCheckReferences() {
		return checkReferences;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

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
		if(sourcePath != null)
			moduleRoot = moduleRoot.child(sourcePath);

		ResultWithDiagnostic<byte[]> result = moduleRoot.act(new ForgeValidatorCallable(
			forgeServiceURL, sourceURI, branch, complianceLevel, checkReferences, checkModuleSemantics, getProblemsAdvisor(),
			getModuleValidationAdvisor().getAdvisor(), puppetLintMaxSeverity, puppetLintOptions));

		// Emit non-validation diagnostics to the console
		boolean validationErrors = false;
		Iterator<Diagnostic> diagIter = result.iterator();
		while(diagIter.hasNext()) {
			Diagnostic diag = diagIter.next();
			if(!(diag instanceof ValidationDiagnostic)) {
				switch(diag.getSeverity()) {
					case Diagnostic.ERROR:
						listener.error(diag.getMessage());
						break;
					case Diagnostic.FATAL:
						listener.fatalError(diag.getMessage());
						break;
					default:
						listener.getLogger().println(diag);
				}
				diagIter.remove();
			}
			else if(diag.getSeverity() == Diagnostic.ERROR)
				validationErrors = true;
		}

		if(validationErrors)
			listener.error("There are validation errors. See Validation Result for details");

		ValidationResult data = new ValidationResult(build);
		build.addAction(data);
		data.setResult(result);
		return result.getSeverity() < Diagnostic.ERROR;
	}
}
