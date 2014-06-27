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

import java.io.Serializable;

import com.puppetlabs.geppetto.module.dsl.validation.IModuleValidationAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

public class ModuleValidationAdvisorBean implements IModuleValidationAdvisor, Serializable {
	private static final long serialVersionUID = 1L;

	private final ValidationPreference circularDependency;

	private final ValidationPreference dependencyVersionMismatch;

	private final ValidationPreference deprecatedKey;

	private final ValidationPreference missingForgeRequiredFields;

	private final ValidationPreference moduleNameNotStrict;

	private final ValidationPreference moduleRedefinition;

	private final ValidationPreference modulefileExists;

	private final ValidationPreference modulefileExistsAndIsUsed;

	private final ValidationPreference unexpectedSubmodule;

	private final ValidationPreference unrecognizedKey;

	private final ValidationPreference unresolvedReference;

	private final ValidationPreference whitespaceInTag;

	public ModuleValidationAdvisorBean(ValidationPreference circularDependency,
			ValidationPreference dependencyVersionMismatch, ValidationPreference deprecatedKey,
			ValidationPreference missingForgeRequiredFields, ValidationPreference moduleNameNotStrict,
			ValidationPreference moduleRedefinition, ValidationPreference modulefileExists,
			ValidationPreference modulefileExistsAndIsUsed, ValidationPreference unexpectedSubmodule,
			ValidationPreference unrecognizedKey, ValidationPreference unresolvedReference,
			ValidationPreference whitespaceInTag) {
		this.circularDependency = circularDependency;
		this.dependencyVersionMismatch = dependencyVersionMismatch;
		this.deprecatedKey = deprecatedKey;
		this.missingForgeRequiredFields = missingForgeRequiredFields;
		this.moduleNameNotStrict = moduleNameNotStrict;
		this.moduleRedefinition = moduleRedefinition;
		this.modulefileExists = modulefileExists;
		this.modulefileExistsAndIsUsed = modulefileExistsAndIsUsed;
		this.unexpectedSubmodule = unexpectedSubmodule;
		this.unrecognizedKey = unrecognizedKey;
		this.unresolvedReference = unresolvedReference;
		this.whitespaceInTag = whitespaceInTag;
	}

	/**
	 * @return the circularDependency
	 */
	public ValidationPreference getCircularDependency() {
		return circularDependency;
	}

	/**
	 * @return the dependencyVersionMismatch
	 */
	public ValidationPreference getDependencyVersionMismatch() {
		return dependencyVersionMismatch;
	}

	/**
	 * @return the deprecatedKey
	 */
	public ValidationPreference getDeprecatedKey() {
		return deprecatedKey;
	}

	/**
	 * @return the missingForgeRequiredFields
	 */
	public ValidationPreference getMissingForgeRequiredFields() {
		return missingForgeRequiredFields;
	}

	/**
	 * @return the modulefileExists
	 */
	public ValidationPreference getModulefileExists() {
		return modulefileExists;
	}

	/**
	 * @return the modulefileExistsAndIsUsed
	 */
	public ValidationPreference getModulefileExistsAndIsUsed() {
		return modulefileExistsAndIsUsed;
	}

	/**
	 * @return the moduleNameNotStrict
	 */
	public ValidationPreference getModuleNameNotStrict() {
		return moduleNameNotStrict;
	}

	/**
	 * @return the moduleRedefinition
	 */
	public ValidationPreference getModuleRedefinition() {
		return moduleRedefinition;
	}

	/**
	 * @return the unexpectedSubmodule
	 */
	public ValidationPreference getUnexpectedSubmodule() {
		return unexpectedSubmodule;
	}

	/**
	 * @return the unrecognizedKey
	 */
	public ValidationPreference getUnrecognizedKey() {
		return unrecognizedKey;
	}

	/**
	 * @return the unresolvedReference
	 */
	public ValidationPreference getUnresolvedReference() {
		return unresolvedReference;
	}

	/**
	 * @return the whitespaceInTag
	 */
	public ValidationPreference getWhitespaceInTag() {
		return whitespaceInTag;
	}

}
