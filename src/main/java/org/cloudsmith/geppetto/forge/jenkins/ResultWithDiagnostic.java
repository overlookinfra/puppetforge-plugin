/*******************************************************************
 * Copyright (c) 2013, Cloudsmith Inc.
 * The code, documentation and other materials contained herein
 * are the sole and exclusive property of Cloudsmith Inc. and may
 * not be disclosed, used, modified, copied or distributed without
 * prior written consent or license from Cloudsmith Inc.
 ******************************************************************/
package org.cloudsmith.geppetto.forge.jenkins;

import java.io.PrintStream;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;

public class ResultWithDiagnostic<T> extends Diagnostic {
	private static final long serialVersionUID = 1618870489419505392L;

	private T result;

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
	 * @param result
	 *            the result to set
	 */
	public void setResult(T result) {
		this.result = result;
	}
}
