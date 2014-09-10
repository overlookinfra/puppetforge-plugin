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

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;

public class ValidationDiagnostic extends Diagnostic {
	private static final long serialVersionUID = -7859501517526959341L;

	private final String hrefPrefix;

	private final String path;

	private final int line;

	public ValidationDiagnostic(int severity, DiagnosticType type, String message, String hrefPrefix, String path, int line) {
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
