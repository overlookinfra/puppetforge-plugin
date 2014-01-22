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
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;

public class ForgePublisher extends Recorder implements Serializable, MatrixAggregatable {
	@Extension(ordinal = -1)
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public String getDisplayName() {
			return "Publish Puppet Module";
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}
	}

	private static final long serialVersionUID = 1L;

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

	@DataBoundConstructor
	public ForgePublisher(Boolean publishOnlyIfNoWarnings, String forgeOAuthToken, String forgeLogin,
			String forgePassword) {
		this.publishOnlyIfNoWarnings = publishOnlyIfNoWarnings == null
				? false
				: publishOnlyIfNoWarnings.booleanValue();
		this.forgeOAuthToken = forgeOAuthToken;
		this.forgeLogin = forgeLogin;
		this.forgePassword = forgePassword;
	}

	/**
	 * For a matrix project, push should only happen once.
	 */
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new MatrixAggregator(build, launcher, listener) {
			@Override
			public boolean endBuild() throws InterruptedException, IOException {
				return ForgePublisher.this.perform(build, launcher, listener);
			}
		};
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

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	public boolean isPublishOnlyIfNoWarnings() {
		return publishOnlyIfNoWarnings;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {

		final Result buildResult = build.getResult();
		if(buildResult.isWorseThan(Result.UNSTABLE))
			// Silently ignore this post-build step
			return true; // Error not caused here

		// during matrix build, the push back would happen at the very end only once for the whole matrix,
		// not for individual configuration build.
		if(build instanceof MatrixRun)
			return true;

		List<ValidationResult> validationResults = build.getActions(ValidationResult.class);
		if(validationResults.isEmpty()) {
			info(listener, "No Puppet Validation Result was found so no pushing will occur.");
			return true; // Error not caused here
		}

		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			info(listener, "Unable to find Git SCM configuration in the project configuration");
			return true; // Error not caused here
		}

		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);
		if(validationResults.size() > 1)
			info(listener, "Got multiple Puppet Validation Results. I'm using the first one.");

		ValidationResult validationResult = validationResults.get(0);
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

		Diagnostic publishingResult = gitRoot.act(new ForgePublisherCallable(
			forgeLogin, forgePassword, validationResult.getRepositoryURL(), validationResult.getBranchName()));
		for(Diagnostic diag : publishingResult)
			listener.getLogger().println(diag);

		PublicationResult data = new PublicationResult(build, publishingResult);
		build.addAction(data);
		return publishingResult.getSeverity() < Diagnostic.ERROR;
	}
}
