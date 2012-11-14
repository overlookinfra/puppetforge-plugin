/**
 * Copyright (c) 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Cloudsmith
 * 
 */
package org.cloudsmith.geppetto.forge.jenkins;

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
import java.io.Serializable;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class ForgePublisher extends Recorder implements Serializable, MatrixAggregatable {
	@Extension(ordinal = -1)
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public String getDisplayName() {
			return "Puppet Forge Publishing";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	private static final long serialVersionUID = 1L;

	private final boolean publishOnlyIfSuccess;

	@DataBoundConstructor
	public ForgePublisher(Boolean publishOnlyIfSuccess) {
		this.publishOnlyIfSuccess = publishOnlyIfSuccess == null
				? true
				: publishOnlyIfSuccess.booleanValue();
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

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	public boolean isPublishOnlyIfSuccess() {
		return publishOnlyIfSuccess;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {

		// during matrix build, the push back would happen at the very end only once for the whole matrix,
		// not for individual configuration build.
		if(build instanceof MatrixRun) {
			return true;
		}

		List<ValidationResult> forgeValidators = build.getActions(ValidationResult.class);
		if(forgeValidators.isEmpty()) {
			listener.error("No Puppet Validation Result was found so no pushing will occur.");
			return false;
		}

		final Result buildResult = build.getResult();

		// If publishOnlyIfSuccess is selected and the build is not a success, don't push.
		if(publishOnlyIfSuccess && buildResult.isWorseThan(Result.SUCCESS)) {
			listener.getLogger().println(
				"Build did not succeed and the project is configured to only push after a successful build, so no pushing will occur.");
			return true;
		}

		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			listener.error("Unable to find Git SCM configuration in the project configuration");
			return false;
		}
		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);
		Diagnostic publishingResult = gitRoot.act(new ForgePublisherCallable(forgeValidators.get(0)));
		for(Diagnostic diag : publishingResult.getChildren())
			listener.getLogger().println(diag);

		PublicationResult data = new PublicationResult(build, publishingResult);
		build.addAction(data);
		return publishingResult.getSeverity() < Diagnostic.ERROR;
	}
}
