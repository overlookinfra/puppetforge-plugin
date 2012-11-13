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

import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import org.apache.http.client.HttpResponseException;
import org.cloudsmith.geppetto.common.os.StreamUtil;
import org.cloudsmith.geppetto.forge.ForgeFactory;
import org.cloudsmith.geppetto.forge.ForgeService;
import org.cloudsmith.geppetto.forge.IncompleteException;
import org.cloudsmith.geppetto.forge.Metadata;
import org.cloudsmith.geppetto.forge.util.TarUtils;
import org.cloudsmith.geppetto.forge.v2.client.ForgePreferences;
import org.cloudsmith.geppetto.forge.v2.service.ReleaseService;

public class ForgePublisher extends ForgeCallable<String> {
	private static final long serialVersionUID = 6670226727298746933L;

	private Collection<String> alreadyPublished;

	public ForgePublisher() {
	}

	public ForgePublisher(ForgePreferences forgePreferences, String repositoryURL, String branchName,
			Collection<String> alreadyPublished) {
		super(forgePreferences, repositoryURL, branchName);
		this.alreadyPublished = alreadyPublished;
	}

	private File buildForge(ForgeService forgeService, File moduleSource, File destination, String[] namesReceiver)
			throws IOException, IncompleteException {
		Metadata md = forgeService.loadModule(moduleSource);
		namesReceiver[0] = md.getUser();
		namesReceiver[1] = md.getName();
		String fullName = md.getFullName();
		if(fullName == null)
			throw new IncompleteException("A full name (user-module) must be specified in the Modulefile");

		String ver = md.getVersion();
		if(ver == null)
			throw new IncompleteException("version must be specified in the Modulefile");

		String fullNameWithVersion = fullName + '-' + ver;
		md.saveJSONMetadata(new File(moduleSource, "metadata.json"));

		File moduleArchive = new File(destination, fullNameWithVersion + ".tar.gz");
		OutputStream out = new GZIPOutputStream(new FileOutputStream(moduleArchive));
		// Pack closes its output
		TarUtils.pack(moduleSource, out, DEFAULT_EXCLUDES_PATTERN, false, fullNameWithVersion);
		return moduleArchive;
	}

	@Override
	protected ResultWithDiagnostic<String> invoke(VirtualChannel channel) throws IOException, InterruptedException {
		ReleaseService releaseService = getForge().createReleaseService();

		ResultWithDiagnostic<String> result = new ResultWithDiagnostic<String>();
		ForgeService forgeService = ForgeFactory.eINSTANCE.createForgeService();
		String[] namesReceiver = new String[2];
		File builtModules = new File(getBuildDir(), "builtModules");
		if(!(builtModules.mkdirs() || builtModules.isDirectory())) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, "Unable to create directory" + builtModules.getPath()));
			return result;
		}

		for(File moduleRoot : findModuleRoots()) {
			if(alreadyPublished.contains(moduleRoot.getAbsolutePath()))
				continue;

			File moduleArchive;
			try {
				moduleArchive = buildForge(forgeService, moduleRoot, builtModules, namesReceiver);
			}
			catch(IncompleteException e) {
				result.addChild(new Diagnostic(Diagnostic.ERROR, e.getMessage()));
				continue;
			}
			InputStream gzInput = new FileInputStream(moduleArchive);
			try {
				releaseService.create(
					namesReceiver[0], namesReceiver[1], "Published using GitHub trigger", gzInput,
					moduleArchive.length());
				result.addChild(new Diagnostic(Diagnostic.INFO, "Module file " + moduleArchive.getName() +
						" has been uploaded"));
			}
			catch(HttpResponseException e) {
				result.addChild(new Diagnostic(Diagnostic.ERROR, "Unable to publish module " + moduleArchive.getName() +
						":" + e.getMessage()));
				continue;
			}
			finally {
				StreamUtil.close(gzInput);
			}
		}
		return result;
	}
}
