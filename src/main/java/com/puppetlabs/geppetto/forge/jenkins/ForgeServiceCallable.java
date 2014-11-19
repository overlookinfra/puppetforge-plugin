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

import java.util.List;

import com.google.inject.Module;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.ForgeService;
import com.puppetlabs.geppetto.forge.client.ForgeHttpModule;
import com.puppetlabs.geppetto.forge.impl.ForgeServiceModule;

public abstract class ForgeServiceCallable<T extends Diagnostic> extends ForgeCallable<T> {
	private static final long serialVersionUID = 1L;

	private String forgeServiceURL;

	public ForgeServiceCallable() {
	}

	public ForgeServiceCallable(String forgeServiceURL, String sourceURI, String branchName) {
		super(sourceURI, branchName);
		this.forgeServiceURL = forgeServiceURL;
	}

	@Override
	protected void addModules(Diagnostic diagnostic, List<Module> modules) {
		super.addModules(diagnostic, modules);
		modules.add(new ForgeHttpModule() {
			@Override
			protected String doGetBaseURL() {
				return forgeServiceURL;
			}
		});
		modules.add(new ForgeServiceModule());
	}

	protected ForgeService getForgeService(Diagnostic diag) {
		return getInjector(diag).getInstance(ForgeService.class);
	}
}
