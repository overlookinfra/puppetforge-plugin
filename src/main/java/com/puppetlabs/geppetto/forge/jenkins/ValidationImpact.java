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

public enum ValidationImpact {
	DO_NOT_FAIL("Never"), FAIL_ON_ALL("On no success"), FAIL_ON_ANY("On any error");

	private final String label;

	private ValidationImpact(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}
}
