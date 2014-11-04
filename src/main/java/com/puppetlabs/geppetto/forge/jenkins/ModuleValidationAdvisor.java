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

import static com.puppetlabs.geppetto.forge.jenkins.ForgeValidator.doFillValidationPreferenceItems;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

import com.puppetlabs.geppetto.module.dsl.validation.DefaultModuleValidationAdvisor;
import com.puppetlabs.geppetto.module.dsl.validation.IModuleValidationAdvisor;
import com.puppetlabs.geppetto.module.dsl.validation.ModuleValidationAdvisorBean;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

/**
 * A default implementation of IPotentialProblemsAdvisor that returns Warnings for all potential problems, and
 * Ignore for all stylistic problems
 */
public class ModuleValidationAdvisor extends AbstractDescribableImpl<ModuleValidationAdvisor> implements ExtensionPoint, Serializable {

	@Extension
	public static class ModuleValidationAdvisorDescriptor extends Descriptor<ModuleValidationAdvisor> {

		public ListBoxModel doFillCircularDependencyItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDependencyVersionMismatchItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDeprecatedKeyItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillMissingForgeRequiredFieldsItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillModuleClassNotInInitPPItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillModulefileExistsAndIsUsedItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillModulefileExistsItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillModuleNameNotStrictItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillModuleRedefinitionItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillUnexpectedSubmoduleItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillUnrecognizedKeyItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillUnresolvedReferenceItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillWhitespaceInTagItems() {
			return doFillValidationPreferenceItems();
		}

		public ModuleValidationAdvisor getDefaults() {
			return defaults;
		}

		@Override
		public String getDisplayName() {
			return "";
		}
	}

	private static final ModuleValidationAdvisor defaults = new ModuleValidationAdvisor();

	private static final long serialVersionUID = 1L;

	private final IModuleValidationAdvisor advisor;

	public ModuleValidationAdvisor() {
		this.advisor = DefaultModuleValidationAdvisor.INSTANCE;
	}

	@DataBoundConstructor
	public ModuleValidationAdvisor(ValidationPreference circularDependency, ValidationPreference dependencyVersionMismatch,
			ValidationPreference deprecatedKey, ValidationPreference missingForgeRequiredFields,
			ValidationPreference moduleClassNotInInitPP, ValidationPreference moduleNameNotStrict, ValidationPreference moduleRedefinition,
			ValidationPreference modulefileExists, ValidationPreference modulefileExistsAndIsUsed,
			ValidationPreference unexpectedSubmodule, ValidationPreference unrecognizedKey, ValidationPreference unresolvedReference,
			ValidationPreference whitespaceInTag) {
		this.advisor = new ModuleValidationAdvisorBean(
			circularDependency, dependencyVersionMismatch, deprecatedKey, missingForgeRequiredFields, moduleClassNotInInitPP,
			moduleNameNotStrict, moduleRedefinition, modulefileExists, modulefileExistsAndIsUsed, unexpectedSubmodule, unrecognizedKey,
			unresolvedReference, whitespaceInTag);
	}

	public IModuleValidationAdvisor getAdvisor() {
		return advisor;
	}

	/**
	 * @return the circularDependency
	 */
	public String getCircularDependency() {
		return advisor.getCircularDependency().name();
	}

	/**
	 * @return the dependencyVersionMismatch
	 */
	public String getDependencyVersionMismatch() {
		return advisor.getDependencyVersionMismatch().name();
	}

	/**
	 * @return the deprecatedKey
	 */
	public String getDeprecatedKey() {
		return advisor.getDeprecatedKey().name();
	}

	/**
	 * @return the missingForgeRequiredFields
	 */
	public String getMissingForgeRequiredFields() {
		return advisor.getMissingForgeRequiredFields().name();
	}

	/**
	 * @return the moduleClassNotInInitPP
	 */
	public String getModuleClassNotInInitPP() {
		return advisor.getModuleClassNotInInitPP().name();
	}

	/**
	 * @return the modulefileExists
	 */
	public String getModulefileExists() {
		return advisor.getModulefileExists().name();
	}

	/**
	 * @return the modulefileExistsAndIsUsed
	 */
	public String getModulefileExistsAndIsUsed() {
		return advisor.getModulefileExistsAndIsUsed().name();
	}

	/**
	 * @return the moduleNameNotStrict
	 */
	public String getModuleNameNotStrict() {
		return advisor.getModuleNameNotStrict().name();
	}

	/**
	 * @return the moduleRedefinition
	 */
	public String getModuleRedefinition() {
		return advisor.getModuleRedefinition().name();
	}

	/**
	 * @return the unexpectedSubmodule
	 */
	public String getUnexpectedSubmodule() {
		return advisor.getUnexpectedSubmodule().name();
	}

	/**
	 * @return the unrecognizedKey
	 */
	public String getUnrecognizedKey() {
		return advisor.getUnrecognizedKey().name();
	}

	/**
	 * @return the unresolvedReference
	 */
	public String getUnresolvedReference() {
		return advisor.getUnresolvedReference().name();
	}

	/**
	 * @return the whitespaceInTag
	 */
	public String getWhitespaceInTag() {
		return advisor.getWhitespaceInTag().name();
	}
}
