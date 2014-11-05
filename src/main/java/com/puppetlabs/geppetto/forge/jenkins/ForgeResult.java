/**
 * Copyright (c) 2014 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 *
 */
package com.puppetlabs.geppetto.forge.jenkins;

import java.util.Map;

public class ForgeResult {
	private Map<String, Map<String, Object>> results;

	private String name;

	private String version;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the results
	 */
	public Map<String, Map<String, Object>> getResults() {
		return results;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param results
	 *            the results to set
	 */
	public void setResults(Map<String, Map<String, Object>> results) {
		this.results = results;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
}
