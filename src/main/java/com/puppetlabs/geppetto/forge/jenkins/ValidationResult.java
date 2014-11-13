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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.model.VersionedName;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;

@ExportedBean(defaultVisibility = 999)
public class ValidationResult implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 2584295798889630530L;

	public static final String RELEASE_PREFIX = "Release: ";

	private ResultWithDiagnostic<byte[]> resultDiagnostic;

	private final AbstractBuild<?, ?> build;

	private final int cardinal;

	public ValidationResult(AbstractBuild<?, ?> build) {
		this.build = build;
		this.cardinal = build.getActions(ValidationResult.class).size();
	}

	@Override
	public ValidationResult clone() {
		ValidationResult clone;
		try {
			clone = (ValidationResult) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException("Error cloning ValidationResult", e);
		}
		return clone;
	}

	/**
	 * Method called by the Stapler dispatcher. It is automatically detected
	 * when the dispatcher looks for methods that starts with &quot;do&quot;
	 * The method doValidation corresponds to the path <build>/stackhammerValidation/dependencyGraph
	 *
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDependencyGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath();
		byte[] result = getResult();
		if((name == null || name.length() == 0) && result != null) {
			rsp.setContentType("image/svg+xml");
			OutputStream out = rsp.getOutputStream();
			try {
				byte[] svgData = result;
				// svgData = GraphTrimmer.stripFixedSize(svgData);
				rsp.setContentLength(svgData.length);
				out.write(svgData);
				return;
			}
			finally {
				out.close();
			}
		}
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public Api getApi() {
		return new Api(this);
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	@Override
	public String getDisplayName() {
		return "Validation Results";
	}

	@Override
	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/puppetforge/icons/puppetlabs-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/puppetforge/icons/puppetlabs-48x48.png";
	}

	/**
	 * Return the slug of the validated module. The slug will only be present when exactly one
	 * module is validated.
	 *
	 * @return The slug of the validated module.
	 */
	public VersionedName getModuleSlug() {
		if(resultDiagnostic != null)
			for(Diagnostic d : resultDiagnostic)
				if(d.getSeverity() == Diagnostic.INFO && d.getMessage().startsWith(RELEASE_PREFIX))
					return new VersionedName(d.getMessage().substring(RELEASE_PREFIX.length()));
		return null;
	}

	private MultiComplianceDiagnostic getMultiDiagnostic() {
		if(resultDiagnostic != null)
			for(Diagnostic d : resultDiagnostic)
				if(d instanceof MultiComplianceDiagnostic)
					return (MultiComplianceDiagnostic) d;
		return new MultiComplianceDiagnostic(PuppetTarget.getDefault().getComplianceLevel());
	}

	public List<ComplianceDiagnostic> getOtherDiagnostics() {
		return getMultiDiagnostic().getOtherDiagnostic();
	}

	public byte[] getResult() {
		return resultDiagnostic == null
			? null
			: resultDiagnostic.getResult();
	}

	public String getResultComplianceLevel() {
		return getMultiDiagnostic().getBest().toString();
	}

	public List<Diagnostic> getResultDiagnostics() {
		ComplianceDiagnostic best = getMultiDiagnostic().getBestDiagnostic();
		return best == null
			? Collections.<Diagnostic> emptyList()
			: best.getChildren();
	}

	public int getResultDiagnosticsCount() {
		return getResultDiagnostics().size();
	}

	public int getSeverity() {
		return resultDiagnostic == null
			? Diagnostic.OK
			: resultDiagnostic.getSeverity();
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

	public String getSummaryValidationGraphURL() {
		return getResult() != null
			? getUrlFor("dependencyGraph")
			: null;
	}

	public String getTitle() {
		return build.getActions(ValidationResult.class).size() <= 1
			? "Validation Result"
			: "Validation Result (" + (cardinal + 1) + ')';
	}

	public List<Diagnostic> getUnfilteredLevelDiagnostics() {
		return getMultiDiagnostic().getChildren();
	}

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	@Override
	public String getUrlName() {
		return build.getActions(ValidationResult.class).size() <= 1
			? "validationReport"
			: "validationReport_" + (cardinal + 1);
	}

	public String getValidationGraphURL() {
		return getResult() != null
			? "dependencyGraph"
			: null;
	}

	public int getWorstLevelSeverity() {
		return getMultiDiagnostic().getWorstSeverity();
	}

	public void setResult(ResultWithDiagnostic<byte[]> resultDiagnostic) {
		this.resultDiagnostic = resultDiagnostic;
	}
}
