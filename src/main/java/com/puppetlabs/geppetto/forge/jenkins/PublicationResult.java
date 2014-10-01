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

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.export.ExportedBean;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;

@ExportedBean(defaultVisibility = 999)
public class PublicationResult implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = -8730291291869073474L;

	private final Diagnostic diagnostic;

	private final AbstractBuild<?, ?> build;

	private final int cardinal;

	public PublicationResult(AbstractBuild<?, ?> build, Diagnostic diagnostic) {
		this.build = build;
		this.diagnostic = diagnostic;
		this.cardinal = build.getActions(PublicationResult.class).size();
	}

	@Override
	public ValidationResult clone() {
		ValidationResult clone;
		try {
			clone = (ValidationResult) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException("Error cloning PublicationResult", e);
		}
		return clone;
	}

	public Api getApi() {
		return new Api(this);
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	@Override
	public String getDisplayName() {
		return "Publication Results";
	}

	@Override
	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/puppetforge/icons/puppetlabs-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/puppetforge/icons/puppetlabs-48x48.png";
	}

	public List<Diagnostic> getResultDiagnostics() {
		return diagnostic == null
			? Collections.<Diagnostic> emptyList()
			: diagnostic.getChildren();
	}

	public int getResultDiagnosticsCount() {
		return getResultDiagnostics().size();
	}

	public String getSummary() {
		return getSummary(getResultDiagnostics());
	}

	protected String getSummary(List<? extends Diagnostic> messages) {
		int errorCount = 0;
		int warningCount = 0;
		for(Diagnostic msg : messages) {
			switch(msg.getSeverity()) {
				case Diagnostic.WARNING:
					warningCount++;
					break;
				case Diagnostic.ERROR:
				case Diagnostic.FATAL:
					errorCount++;
			}
		}

		if(errorCount == 0) {
			if(warningCount == 0)
				return "No errors or warnings";
			return warningCount + (warningCount > 1
				? " warnings"
				: " warning");
		}

		if(warningCount == 0)
			return errorCount + (errorCount > 1
				? " errors"
				: " error");
		return errorCount + (errorCount > 1
			? " errors and "
			: " error and ") + warningCount + (warningCount > 1
			? " warnings"
			: " warning");
	}

	public String getTitle() {
		return build.getActions(PublicationResult.class).size() <= 1
			? "Publication Result"
			: "Publication Result (" + (cardinal + 1) + ')';
	}

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	@Override
	public String getUrlName() {
		return build.getActions(PublicationResult.class).size() <= 1
			? "publicationReport"
			: "publicationReport" + (cardinal + 1);
	}
}
