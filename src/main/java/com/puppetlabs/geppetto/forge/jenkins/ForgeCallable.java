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

import static com.puppetlabs.geppetto.injectable.CommonModuleProvider.getCommonModule;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.Forge;
import com.puppetlabs.geppetto.forge.impl.ForgeModule;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.forge.util.ModuleUtils;

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

	private transient Repository localRepository;

	private transient File repositoryDir;

	private String repositoryURL;

	private String branchName;

	public ForgeCallable() {
	}

	public ForgeCallable(String repositoryURL, String branchName) {
		this.repositoryURL = repositoryURL;
		this.branchName = branchName;
	}

	protected void addModules(Diagnostic diagnostic, List<Module> modules) {
		modules.add(new ForgeModule());
		modules.add(getCommonModule());
	}

	protected Collection<File> findModuleRoots(Diagnostic diag) {
		return getForge(diag).findModuleRoots(getRepositoryDir(), getFileFilter());
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

	protected Forge getForge(Diagnostic diag) {
		return getInjector(diag).getInstance(Forge.class);
	}

	synchronized Injector getInjector(Diagnostic diag) {
		if(injector == null) {
			List<Module> modules = new ArrayList<Module>();
			addModules(diag, modules);
			if(diag.getSeverity() <= Diagnostic.WARNING) {
				injector = Guice.createInjector(modules);
			}
		}
		return injector;
	}

	protected Repository getLocalRepository() throws IOException {
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
		return getForge(diag).createFromModuleDirectory(moduleDirectory, true, null, null, diag);
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
