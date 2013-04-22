/**
 * Copyright 2012-, Cloudsmith Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.cloudsmith.geppetto.forge.jenkins;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class PublicationResult implements Action, Serializable, Cloneable {

	private final Diagnostic diagnostic;

	private final AbstractBuild<?, ?> build;

	public PublicationResult(AbstractBuild<?, ?> build, Diagnostic diagnostic) {
		this.build = build;
		this.diagnostic = diagnostic;
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

	public String getDisplayName() {
		return "Publication Results";
	}

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
		return "Publication Result";
	}

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	public String getUrlName() {
		return "publicationReport";
	}
}
