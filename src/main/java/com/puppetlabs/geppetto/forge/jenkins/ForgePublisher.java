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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.jenkins.ForgeBuilder.RepositoryInfo;

public class ForgePublisher extends Builder {

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public FormValidation doCheckForgeServiceURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		@Override
		public String getDisplayName() {
			return "Publish Puppet Module";
		}

		/**
		 * @return the default Forge Service URL
		 */
		public String getForgeServiceURL() {
			return FORGE_SERVICE_URL;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	private static void info(BuildListener listener, String fmt, Object... args) {
		PrintStream out = listener.getLogger();
		out.print("INFO:");
		out.format(fmt, args);
		out.println();
	}

	private final boolean publishOnlyIfNoWarnings;

	private final String forgeOAuthToken;

	private final String forgeLogin;

	private final String forgePassword;

	private final String forgeServiceURL;

	@DataBoundConstructor
	public ForgePublisher(Boolean publishOnlyIfNoWarnings, String forgeOAuthToken, String forgeServiceURL,
			String forgeLogin, String forgePassword) {
		this.forgeServiceURL = forgeServiceURL == null
				? FORGE_SERVICE_URL
				: forgeServiceURL;
		this.publishOnlyIfNoWarnings = publishOnlyIfNoWarnings == null
				? false
				: publishOnlyIfNoWarnings.booleanValue();
		this.forgeOAuthToken = forgeOAuthToken;
		this.forgeLogin = forgeLogin;
		this.forgePassword = forgePassword;
	}

	public String getForgeLogin() {
		return forgeLogin;
	}

	public String getForgeOAuthToken() {
		return forgeOAuthToken;
	}

	public String getForgePassword() {
		return forgePassword;
	}

	public String getForgeServiceURL() {
		return forgeServiceURL;
	}

	public boolean isPublishOnlyIfNoWarnings() {
		return publishOnlyIfNoWarnings;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {

		RepositoryInfo repoInfo = getRepositoryInfo(build, listener);
		if(repoInfo == null)
			return false;

		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			info(listener, "Unable to find Git SCM configuration in the project configuration");
			return true; // Error not caused here
		}

		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);

		ValidationResult validationResult = null;
		List<ValidationResult> validationResults = build.getActions(ValidationResult.class);
		if(!validationResults.isEmpty()) {
			if(validationResults.size() > 1)
				info(listener, "Got multiple Puppet Validation Results. I'm using the first one.");

			validationResult = validationResults.get(0);
			int severity = validationResult.getSeverity();
			if(severity >= Diagnostic.ERROR) {
				// This should normally never happen since the build result should have been worse
				// than stable and trapped above.
				info(listener, "Validation of %s has errors so no pushing will occur.", gitRoot.getName());
				return true; // Error not caused here
			}

			if(severity == Diagnostic.WARNING && publishOnlyIfNoWarnings) {
				listener.error("Validation of %s has warnings so no pushing will occur.", gitRoot.getName());
				return false; // Fail the build here
			}
		}

		Diagnostic publishingResult = gitRoot.act(new ForgePublisherCallable(
			forgeLogin, forgePassword, forgeServiceURL, repoInfo.repositoryURL, repoInfo.branchName));

		PublicationResult data = new PublicationResult(build, publishingResult);
		build.addAction(data);
		return publishingResult.getSeverity() < Diagnostic.ERROR;
	}
}
