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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Option;

public class ForgeValidator extends Builder {

	/**
	 * Descriptor for {@link ForgeValidator}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See <tt>src/main/resources/com/puppetlabs/geppetto/jenkins/ForgeValidator/*.jelly</tt> for the actual HTML fragment for the
	 * configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private static FormValidation checkURL(String value) {
			try {
				URI uri = new URI(value);
				if(!uri.isAbsolute())
					return FormValidation.error("URL must be absolute");

				if(uri.isOpaque())
					return FormValidation.error("URL must not be opaque");

				uri.toURL();
				return FormValidation.ok();
			}
			catch(MalformedURLException e) {
				return FormValidation.error(e, "Not a valid URL");
			}
			catch(URISyntaxException e) {
				return FormValidation.error(e, "Not a valid URL");
			}
		}

		private String serviceURL = FORGE_SERVICE_URL;

		public DescriptorImpl() {
			super(ForgeValidator.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckServiceURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return "Validate Puppet Module";
		}

		public String getServiceURL() {
			return serviceURL;
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		public void setServiceURL(String serviceURL) {
			this.serviceURL = serviceURL;
		}
	}

	static final String FORGE_SERVICE_URL = "http://forgeapi.puppetlabs.com";

	public static DiagnosticType VALIDATOR_TYPE = new DiagnosticType("VALIDATOR", ForgeValidator.class.getName());

	public static final String ALREADY_PUBLISHED = "ALREADY_PUBLISHED";

	static String getForgeServiceURL() {
		DescriptorImpl desc = (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(ForgeValidator.class);
		return desc.getServiceURL();
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

	/**
	 * Obtains the remote URL that is referenced by the given <code>branchName</code>
	 * 
	 * @return The URL or <code>null</code> if it hasn't been configured
	 *         for the given branch.
	 */
	public static String getRemoteURL(FileRepository repository) throws IOException {
		StoredConfig repoConfig = repository.getConfig();
		String configuredRemote = repoConfig.getString(
			ConfigConstants.CONFIG_BRANCH_SECTION, repository.getBranch(), ConfigConstants.CONFIG_KEY_REMOTE);
		return configuredRemote == null
				? null
				: repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, configuredRemote, ConfigConstants.CONFIG_KEY_URL);
	}

	private final boolean checkReferences;

	private final boolean checkModuleSemantics;

	private final String puppetLintOptions;

	@DataBoundConstructor
	public ForgeValidator(Boolean checkReferences, Boolean checkModuleSemantics, String puppetLintOptions) {
		this.checkReferences = checkReferences == null
				? false
				: checkReferences.booleanValue();
		this.checkModuleSemantics = checkModuleSemantics == null
				? false
				: checkModuleSemantics.booleanValue();

		if(puppetLintOptions != null) {
			puppetLintOptions = puppetLintOptions.trim();
			if(puppetLintOptions.length() == 0)
				puppetLintOptions = null;
		}
		this.puppetLintOptions = puppetLintOptions;
	}

	public String getPuppetLintOptions() {
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

		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			listener.error("Unable to find Git SCM configuration in the project configuration");
			return false;
		}
		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);
		List<UserRemoteConfig> repos = git.getUserRemoteConfigs();
		if(repos.size() == 0) {
			listener.error("Unable to find the Git repository URL");
			return false;
		}
		if(repos.size() > 1) {
			listener.error("Sorry, but publishing from multiple repositories is currently not supported");
			return false;
		}
		String repository = repos.get(0).getUrl();

		List<BranchSpec> branches = git.getBranches();
		String branchName = null;
		if(branches.size() == 0)
			branchName = Constants.MASTER;
		else if(branches.size() == 1) {
			BranchSpec branchSpec = branches.get(0);
			branchName = branchSpec.getName();
			if("**".equals(branchName))
				branchName = Constants.MASTER;
		}
		else {
			listener.error("Sorry, but publishing from multiple branches is not supported");
			return false;
		}

		Option[] options;
		if(puppetLintOptions == null)
			options = null;
		else {
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
					listener.error(e.getMessage());
				}
				start = commaIdx + 1;
			}
			if(start < top)
				try {
					optionList.add(getOption(puppetLintOptions.substring(start)));
				}
				catch(IllegalArgumentException e) {
					allValid = false;
					listener.error(e.getMessage());
				}

			if(!allValid)
				return false;

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

		boolean validationErrors = false;
		ResultWithDiagnostic<byte[]> result = gitRoot.act(new ForgeValidatorCallable(
			repository, branchName, checkReferences, checkModuleSemantics, options));

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

		ValidationResult data = new ValidationResult(build, repository, branchName);
		build.addAction(data);
		data.setResult(result);
		return result.getSeverity() < Diagnostic.ERROR;
	}
}
