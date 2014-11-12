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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.model.Type;
import com.puppetlabs.geppetto.forge.model.VersionedName;

public class ResultWithDiagnostic<T> extends Diagnostic {
	private static final long serialVersionUID = 1618870489419505392L;

	private T result;

	private Map<VersionedName, Collection<Type>> extractedTypes;

	/**
	 * @return the extractedTypes
	 */
	public Map<VersionedName, Collection<Type>> getExtractedTypes() {
		return extractedTypes == null
			? Collections.<VersionedName, Collection<Type>> emptyMap()
			: extractedTypes;
	}

	/**
	 * @return the result
	 */
	public T getResult() {
		return result;
	}

	/**
	 * Print the diagnostic children as a log output on the given <code>logger</code>
	 *
	 * @param logger
	 */
	public void log(PrintStream logger) {
		for(Diagnostic child : this)
			logger.println(child);
	}

	/**
	 * @param extractedTypes
	 */
	public void setExtractedTypes(Map<VersionedName, Collection<Type>> extractedTypes) {
		this.extractedTypes = extractedTypes;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(T result) {
		this.result = result;
	}
}
