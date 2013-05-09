/*******************************************************************
 * Copyright (c) 2013, Cloudsmith Inc.
 * The code, documentation and other materials contained herein
 * are the sole and exclusive property of Cloudsmith Inc. and may
 * not be disclosed, used, modified, copied or distributed without
 * prior written consent or license from Cloudsmith Inc.
 ******************************************************************/
package org.cloudsmith.geppetto.forge.jenkins;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.geppetto.diagnostic.DiagnosticType;

public class ValidationDiagnostic extends Diagnostic {
	private static final long serialVersionUID = -7859501517526959341L;

	private final String hrefPrefix;

	private final String path;

	private final int line;

	public ValidationDiagnostic(int severity, DiagnosticType type, String message, String hrefPrefix, String path,
			int line) {
		super(severity, type, message);
		this.hrefPrefix = hrefPrefix;
		this.path = path;
		this.line = line;
	}

	public String getHrefPrefix() {
		return hrefPrefix;
	}

	public int getLine() {
		return line;
	}

	public String getPath() {
		return path;
	}
}
