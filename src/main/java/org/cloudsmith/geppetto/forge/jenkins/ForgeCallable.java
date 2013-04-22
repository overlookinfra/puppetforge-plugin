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

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.geppetto.forge.Forge;
import org.cloudsmith.geppetto.forge.ForgePreferences;
import org.cloudsmith.geppetto.forge.ForgeService;
import org.cloudsmith.geppetto.forge.util.ModuleUtils;
import org.cloudsmith.geppetto.forge.v2.model.Metadata;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.google.inject.Injector;
import com.google.inject.Module;

public abstract class ForgeCallable<T> implements FileCallable<T> {
	private static final long serialVersionUID = -3048930993120683688L;

	public static final String BUILD_DIR = ".geppetto";

	public static final String IMPORTED_MODULES_ROOT = "importedModules";

	public static boolean isParentOrEqual(File dir, File subdir) {
		if(dir == null || subdir == null)
			return false;

		return dir.equals(subdir) || isParentOrEqual(dir, subdir.getParentFile());
	}

	private transient File buildDir;

	private transient Injector injector;

	private transient FileRepository localRepository;

	private transient File repositoryDir;

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

	abstract Injector createInjector(Module module);

	protected Collection<File> findModuleRoots() {
		return getForge().findModuleRoots(getRepositoryDir(), getFileFilter());
	}

	public String getBranchName() {
		return branchName;
	}

	protected File getBuildDir() {
		return buildDir;
	}

	/**
	 * Returns an exclusion filter that rejects everything beneath the build directory plus everything that
	 * the default exclusion filter would reject.
	 * 
	 * @return <tt>true</tt> if the file can be accepted for inclusion
	 */
	protected FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(File file) {
				return ModuleUtils.DEFAULT_FILE_FILTER.accept(file) && !isParentOrEqual(getBuildDir(), file);
			}
		};
	}

	protected Forge getForge() {
		return getInjector().getInstance(Forge.class);
	}

	synchronized Injector getInjector() {
		if(injector == null)
			injector = createInjector(new ForgeService(forgePreferences).getForgeModule());
		return injector;
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

	protected Metadata getModuleMetadata(File moduleDirectory, Diagnostic diag) throws IOException {
		return getForge().createFromModuleDirectory(moduleDirectory, true, null, null, diag);
	}

	protected File getRepositoryDir() {
		return repositoryDir;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public final T invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		repositoryDir = f;
		buildDir = new File(f, BUILD_DIR);
		return invoke(channel);
	}

	protected abstract T invoke(VirtualChannel channel) throws IOException, InterruptedException;
}
