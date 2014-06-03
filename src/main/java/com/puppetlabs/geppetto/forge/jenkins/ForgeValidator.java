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
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.checkURL;
import static com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.getRepositoryInfo;
import hudson.Extension;
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

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;
import com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.RepositoryInfo;
import com.puppetlabs.geppetto.forge.jenkins.ProblemsAdvisor.ProblemsAdvisorDescriptor;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Option;

public class ForgeValidator extends Builder {

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
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

		public ListBoxModel doFillComplianceLevelItems() {
			ListBoxModel items = new ListBoxModel();
			for(ComplianceLevel level : ComplianceLevel.values())
				items.add(level.toString(), level.name());
			return items;
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

		public Descriptor<ProblemsAdvisor> getProblemsAdvisorDescriptor() {
			return Jenkins.getInstance().getDescriptorByType(ProblemsAdvisorDescriptor.class);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	public static DiagnosticType VALIDATOR_TYPE = new DiagnosticType("VALIDATOR", ForgeValidator.class.getName());

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
		Option[] options;
		if(puppetLintOptions == null)
			options = null;
		else {
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
			options = optionList.toArray(new Option[optionList.size()]);
		}
		return options;
	}

	private final ComplianceLevel complianceLevel;

	private final boolean checkReferences;

	private final boolean checkModuleSemantics;

	private final ProblemsAdvisor problemsAdvisor;

	private final Option[] puppetLintOptions;

	private final String forgeServiceURL;

	@DataBoundConstructor
	public ForgeValidator(String forgeServiceURL, ComplianceLevel complianceLevel, Boolean checkReferences,
			Boolean checkModuleSemantics, ProblemsAdvisor problemsAdvisor, String puppetLintOptions)
			throws FormValidation {
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

		if(puppetLintOptions != null) {
			puppetLintOptions = puppetLintOptions.trim();
			if(puppetLintOptions.length() == 0)
				puppetLintOptions = null;
		}
		this.puppetLintOptions = parsePuppetLintOptions(puppetLintOptions);
	}

	public String getComplianceLevel() {
		return complianceLevel.name();
	}

	public String getForgeServiceURL() {
		return forgeServiceURL;
	}

	public ProblemsAdvisor getProblemsAdvisor() {
		return problemsAdvisor == null
				? new ProblemsAdvisor(new DefaultPotentialProblemsAdvisor())
				: problemsAdvisor;
	}

	public Option[] getPuppetLintOptions() {
		return puppetLintOptions;
	}

	public boolean isCheckModuleSemantics() {
		return checkModuleSemantics;
	}

	public boolean isCheckReferences() {
		return checkReferences;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		RepositoryInfo repoInfo = getRepositoryInfo(build, listener);
		if(repoInfo == null)
			return false;

		boolean validationErrors = false;
		ResultWithDiagnostic<byte[]> result = repoInfo.gitRoot.act(new ForgeValidatorCallable(
			forgeServiceURL, repoInfo.repositoryURL, repoInfo.branchName, complianceLevel, checkReferences,
			checkModuleSemantics, problemsAdvisor, puppetLintOptions));

		// Emit non-validation diagnostics to the console
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
