/*******************************************************************
 * Copyright (c) 2013, Cloudsmith Inc.
 * The code, documentation and other materials contained herein
 * are the sole and exclusive property of Cloudsmith Inc. and may
 * not be disclosed, used, modified, copied or distributed without
 * prior written consent or license from Cloudsmith Inc.
 ******************************************************************/
package org.cloudsmith.geppetto.forge.jenkins;

import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.geppetto.forge.Forge;
import org.cloudsmith.geppetto.forge.ForgePreferences;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ForgePublisherCallable extends ForgeCallable<Diagnostic> {
	private static final long serialVersionUID = -5323808336225028893L;

	public ForgePublisherCallable() {
	}

	public ForgePublisherCallable(ForgePreferences forgePreferences, String repositoryURL, String branchName) {
		super(forgePreferences, repositoryURL, branchName);
	}

	@Override
	Injector createInjector(Module module) {
		return Guice.createInjector(module);
	}

	@Override
	protected Diagnostic invoke(VirtualChannel channel) throws IOException, InterruptedException {
		Diagnostic result = new Diagnostic();
		Collection<File> moduleRoots = findModuleRoots();
		if(moduleRoots.isEmpty()) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, Forge.PACKAGE, "No modules found in repository"));
			return result;
		}

		File buildDir = getBuildDir();
		buildDir.mkdirs();
		File builtModules = new File(buildDir, "builtModules");
		if(!(builtModules.mkdir() || builtModules.isDirectory())) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, Forge.PACKAGE, "Unable to create directory" +
					builtModules.getPath()));
			return result;
		}

		List<File> tarBalls = new ArrayList<File>();
		Forge forge = getForge();
		for(File moduleRoot : moduleRoots)
			tarBalls.add(forge.build(moduleRoot, builtModules, null, null, result));

		getForge().publishAll(tarBalls.toArray(new File[tarBalls.size()]), false, result);
		return result;
	}
}
