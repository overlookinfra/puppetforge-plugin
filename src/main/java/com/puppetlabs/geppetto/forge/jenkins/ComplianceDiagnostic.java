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

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;

/**
 * Diagnostic per-compilance grouping
 */
public class ComplianceDiagnostic extends Diagnostic {
	private static final long serialVersionUID = 1L;

	private final ComplianceLevel complianceLevel;

	public ComplianceDiagnostic(ComplianceLevel complianceLevel) {
		super();
		this.complianceLevel = complianceLevel;
	}

	/**
	 * @return the complianceLevel
	 */
	public ComplianceLevel getComplianceLevel() {
		return complianceLevel;
	}
}
