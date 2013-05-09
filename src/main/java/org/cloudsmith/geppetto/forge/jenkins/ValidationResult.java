/*******************************************************************
 * Copyright (c) 2013, Cloudsmith Inc.
 * The code, documentation and other materials contained herein
 * are the sole and exclusive property of Cloudsmith Inc. and may
 * not be disclosed, used, modified, copied or distributed without
 * prior written consent or license from Cloudsmith Inc.
 ******************************************************************/
package org.cloudsmith.geppetto.forge.jenkins;

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

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.geppetto.forge.ForgePreferences;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class ValidationResult implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 2584295798889630530L;

	private ResultWithDiagnostic<byte[]> resultDiagnostic;

	private final AbstractBuild<?, ?> build;

	private final org.cloudsmith.geppetto.forge.ForgePreferences forgePreferences;

	private final String branchName;

	private final String repositoryURL;

	public ValidationResult(AbstractBuild<?, ?> build, ForgePreferences forgePreferences, String repositoryURL,
			String branchName) {
		this.build = build;
		this.forgePreferences = forgePreferences;
		this.repositoryURL = repositoryURL;
		this.branchName = branchName;
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

	public String getBranchName() {
		return branchName;
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	public String getDisplayName() {
		return "Validation Results";
	}

	public ForgePreferences getForgePreferences() {
		return forgePreferences;
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/puppetforge/icons/puppetlabs-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/puppetforge/icons/puppetlabs-48x48.png";
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public byte[] getResult() {
		return resultDiagnostic == null
				? null
				: resultDiagnostic.getResult();
	}

	public List<Diagnostic> getResultDiagnostics() {
		return resultDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: resultDiagnostic.getChildren();
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
		return "Validation Result";
	}

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	public String getUrlName() {
		return "validationReport";
	}

	public String getValidationGraphURL() {
		return getResult() != null
				? "dependencyGraph"
				: null;
	}

	public void setResult(ResultWithDiagnostic<byte[]> resultDiagnostic) {
		this.resultDiagnostic = resultDiagnostic;
	}
}
