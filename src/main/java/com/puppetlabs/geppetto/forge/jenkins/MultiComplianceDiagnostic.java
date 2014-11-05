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
 * Diagnostic that will have severity INFO regardless of the severity of its children.
 */
public class MultiComplianceDiagnostic extends Diagnostic {
	private static final long serialVersionUID = 1L;

	private final ComplianceLevel best;

	MultiComplianceDiagnostic(ComplianceLevel best) {
		this.best = best;
	}

	/**
	 * @return the best compliance level
	 */
	public ComplianceLevel getBest() {
		return best;
	}

	@Override
	public int getSeverity() {
		return Diagnostic.INFO;
	}
}
