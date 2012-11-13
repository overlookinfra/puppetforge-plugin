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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class ValidationResult implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private ResultWithDiagnostic<byte[]> resultDiagnostic;

	private final AbstractBuild<?, ?> build;

	public ValidationResult(AbstractBuild<?, ?> build) {
		this.build = build;
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

	public String getDisplayName() {
		return "Validation Results";
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/puppetforge/icons/puppetlabs-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/puppetforge/icons/puppetlabs-48x48.png";
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

	public String getSummary() {
		return getSummary(getResultDiagnostics());
	}

	protected String getSummary(List<? extends MessageWithSeverity> messages) {
		int errorCount = 0;
		int warningCount = 0;
		for(MessageWithSeverity msg : messages) {
			switch(msg.getSeverity()) {
				case MessageWithSeverity.WARNING:
					warningCount++;
					break;
				case MessageWithSeverity.ERROR:
				case MessageWithSeverity.FATAL:
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
