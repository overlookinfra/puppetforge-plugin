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

import static org.cloudsmith.geppetto.forge.impl.MetadataImpl.DEFAULT_EXCLUDES_PATTERN;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudsmith.geppetto.forge.v2.Forge;
import org.cloudsmith.geppetto.forge.v2.client.ForgePreferences;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public abstract class ForgeCallable<T> implements FileCallable<ResultWithDiagnostic<T>> {
	public static final String IMPORTED_MODULES_ROOT = "importedModules";

	private transient Forge forge;

	private transient File repositoryDir;

	private transient FileRepository localRepository;

	private ForgePreferences forgePreferences;

	private String repositoryURL;

	private String branchName;

	public ForgeCallable() {
	}

	public ForgeCallable(ForgePreferences forgePreferences, String repositoryURL, String branchName) {
		this.forgePreferences = forgePreferences;
		this.repositoryURL = repositoryURL;
		this.branchName = branchName;
	}

	private boolean findModuleFiles(File[] files, List<File> moduleFiles) {
		if(files != null) {
			int idx = files.length;
			while(--idx >= 0)
				if("Modulefile".equals(files[idx].getName()))
					return true;

			idx = files.length;
			while(--idx >= 0) {
				File file = files[idx];
				String name = file.getName();
				if(IMPORTED_MODULES_ROOT.equals(name) || DEFAULT_EXCLUDES_PATTERN.matcher(name).matches())
					continue;

				if(findModuleFiles(file.listFiles(), moduleFiles))
					moduleFiles.add(file);
			}
		}
		return false;
	}

	protected List<File> findModuleRoots() {
		// Scan for valid directories containing "Modulefile" files.

		List<File> moduleRoots = new ArrayList<File>();
		if(findModuleFiles(repositoryDir.listFiles(), moduleRoots))
			// The repository is a module in itself
			moduleRoots.add(repositoryDir);
		return moduleRoots;
	}

	public String getBranchName() {
		return branchName;
	}

	protected synchronized Forge getForge() {
		if(forge == null)
			forge = new Forge(forgePreferences);
		return forge;
	}

	protected FileRepository getLocalRepository() throws IOException {
		if(localRepository == null) {
			File guessGitDir = new File(getRepositoryDir(), ".git");
			boolean bare = !guessGitDir.isDirectory();
			FileRepositoryBuilder bld = new FileRepositoryBuilder();
			if(bare) {
				bld.setBare();
				bld.setGitDir(getRepositoryDir());
			}
			else
				bld.setWorkTree(getRepositoryDir());
			localRepository = bld.setup().build();
		}
		return localRepository;
	}

	protected File getRepositoryDir() {
		return repositoryDir;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public final ResultWithDiagnostic<T> invoke(File f, VirtualChannel channel) throws IOException,
			InterruptedException {
		repositoryDir = f;
		return invoke(channel);
	}

	protected abstract ResultWithDiagnostic<T> invoke(VirtualChannel channel) throws IOException, InterruptedException;
}
