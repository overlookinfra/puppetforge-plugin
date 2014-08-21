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

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

abstract class ForgeBuilder {
	static class RepositoryInfo {
		final String repositoryURL;

		final String branchName;

		final FilePath gitRoot;

		RepositoryInfo(String repositoryURL, String branchName, FilePath gitRoot) {
			this.repositoryURL = repositoryURL;
			this.branchName = branchName;
			this.gitRoot = gitRoot;
		}
	}

	static FormValidation checkURL(String value) {
		if(value == null)
			return FormValidation.ok();
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

	/**
	 * Obtains the remote URL that is referenced by the given <code>branchName</code>
	 *
	 * @return The URL or <code>null</code> if it hasn't been configured
	 *         for the given branch.
	 */
	static String getRemoteURL(Repository repository) throws IOException {
		StoredConfig repoConfig = repository.getConfig();
		String configuredRemote = repoConfig.getString(
			ConfigConstants.CONFIG_BRANCH_SECTION, repository.getBranch(), ConfigConstants.CONFIG_KEY_REMOTE);
		return configuredRemote == null
				? null
				: repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, configuredRemote, ConfigConstants.CONFIG_KEY_URL);
	}

	static RepositoryInfo getRepositoryInfo(AbstractBuild<?, ?> build, BuildListener listener) {
		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			listener.error("Unable to find Git SCM configuration in the project configuration");
			return null;
		}
		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);
		List<UserRemoteConfig> repos = git.getUserRemoteConfigs();
		if(repos.size() == 0) {
			listener.error("Unable to find the Git repository URL");
			return null;
		}
		if(repos.size() > 1) {
			listener.error("Sorry, but publishing from multiple repositories is currently not supported");
			return null;
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
			return null;
		}
		return new RepositoryInfo(repository, branchName, gitRoot);
	}

	static final String FORGE_SERVICE_URL = "https://forgeapi.puppetlabs.com";

	public static final String ALREADY_PUBLISHED = "ALREADY_PUBLISHED";
}
