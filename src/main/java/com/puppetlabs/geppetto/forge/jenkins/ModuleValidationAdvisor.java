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

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.puppetlabs.geppetto.module.dsl.validation.DefaultModuleValidationAdvisor;
import com.puppetlabs.geppetto.module.dsl.validation.IModuleValidationAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

/**
 * A default implementation of IPotentialProblemsAdvisor that returns Warnings for all potential problems, and
 * Ignore for all stylistic problems
 */
public class ModuleValidationAdvisor extends AbstractDescribableImpl<ModuleValidationAdvisor> implements
		ExtensionPoint, Serializable {

	@Extension
	public static class ModuleValidationAdvisorDescriptor extends Descriptor<ModuleValidationAdvisor> {

		public ListBoxModel doFillCircularDependencyItems() {
			return doFillValidationPreferenceItems(defaults.getCircularDependency());
		}

		public ListBoxModel doFillDependencyVersionMismatchItems() {
			return doFillValidationPreferenceItems(defaults.getDependencyVersionMismatch());
		}

		public ListBoxModel doFillDeprecatedKeyItems() {
			return doFillValidationPreferenceItems(defaults.getDeprecatedKey());
		}

		public ListBoxModel doFillMissingForgeRequiredFieldsItems() {
			return doFillValidationPreferenceItems(defaults.getMissingForgeRequiredFields());
		}

		public ListBoxModel doFillModulefileExistsAndIsUsedItems() {
			return doFillValidationPreferenceItems(defaults.getModulefileExistsAndIsUsed());
		}

		public ListBoxModel doFillModulefileExistsItems() {
			return doFillValidationPreferenceItems(defaults.getModulefileExists());
		}

		public ListBoxModel doFillModuleNameNotStrictItems() {
			return doFillValidationPreferenceItems(defaults.getModuleNameNotStrict());
		}

		public ListBoxModel doFillModuleRedefinitionItems() {
			return doFillValidationPreferenceItems(defaults.getModuleRedefinition());
		}

		public ListBoxModel doFillUnexpectedSubmoduleItems() {
			return doFillValidationPreferenceItems(defaults.getUnexpectedSubmodule());
		}

		public ListBoxModel doFillUnrecognizedKeyItems() {
			return doFillValidationPreferenceItems(defaults.getUnrecognizedKey());
		}

		public ListBoxModel doFillUnresolvedReferenceItems() {
			return doFillValidationPreferenceItems(defaults.getUnresolvedReference());
		}

		public ListBoxModel doFillValidationPreferenceItems(ValidationPreference dflt) {
			List<Option> items = new ArrayList<Option>();
			for(ValidationPreference pref : ValidationPreference.values())
				items.add(new Option(pref.toString(), pref.name(), pref == dflt));
			return new ListBoxModel(items);
		}

		public ListBoxModel doFillWhitespaceInTagItems() {
			return doFillValidationPreferenceItems(defaults.getWhitespaceInTag());
		}

		public IModuleValidationAdvisor getDefaults() {
			return defaults;
		}

		@Override
		public String getDisplayName() {
			return "";
		}
	}

	private static final IModuleValidationAdvisor defaults = new DefaultModuleValidationAdvisor();

	private static final long serialVersionUID = 1L;

	private IModuleValidationAdvisor advisor;

	public ModuleValidationAdvisor() {
		this.advisor = defaults;
	}

	@DataBoundConstructor
	public ModuleValidationAdvisor(ValidationPreference circularDependency,
			ValidationPreference dependencyVersionMismatch, ValidationPreference deprecatedKey,
			ValidationPreference missingForgeRequiredFields, ValidationPreference moduleNameNotStrict,
			ValidationPreference moduleRedefinition, ValidationPreference modulefileExists,
			ValidationPreference modulefileExistsAndIsUsed, ValidationPreference unexpectedSubmodule,
			ValidationPreference unrecognizedKey, ValidationPreference unresolvedReference,
			ValidationPreference whitespaceInTag) {
		this.advisor = new ModuleValidationAdvisorBean(
			circularDependency, dependencyVersionMismatch, deprecatedKey, missingForgeRequiredFields,
			moduleNameNotStrict, moduleRedefinition, modulefileExists, modulefileExistsAndIsUsed, unexpectedSubmodule,
			unrecognizedKey, unresolvedReference, whitespaceInTag);
	}

	public IModuleValidationAdvisor getAdvisor() {
		return advisor;
	}

	/**
	 * @return the assignmentToVarNamedString
	 */
	public String getAssignmentToVarNamedString() {
		return advisor.getCircularDependency().name();
	}

	/**
	 * @return the assignmentToVarNamedTrusted
	 */
	public String getAssignmentToVarNamedTrusted() {
		return advisor.getDependencyVersionMismatch().name();
	}

	/**
	 * @return the booleansInStringForm
	 */
	public String getBooleansInStringForm() {
		return advisor.getDeprecatedKey().name();
	}

	/**
	 * @return the caseDefaultShouldAppearLast
	 */
	public String getCaseDefaultShouldAppearLast() {
		return advisor.getMissingForgeRequiredFields().name();
	}

	/**
	 * @return the dqStringNotRequired
	 */
	public String getDqStringNotRequired() {
		return advisor.getModulefileExists().name();
	}

	/**
	 * @return the dqStringNotRequiredVariable
	 */
	public String getDqStringNotRequiredVariable() {
		return advisor.getModulefileExistsAndIsUsed().name();
	}

	/**
	 * @return the ensureShouldAppearFirstInResource
	 */
	public String getEnsureShouldAppearFirstInResource() {
		return advisor.getModuleNameNotStrict().name();
	}

	/**
	 * @return the interpolatedNonBraceEnclosedHyphens
	 */
	public String getInterpolatedNonBraceEnclosedHyphens() {
		return advisor.getModuleRedefinition().name();
	}

	/**
	 * @return the missingDefaultInSelector
	 */
	public String getMissingDefaultInSelector() {
		return advisor.getUnexpectedSubmodule().name();
	}

	/**
	 * @return the mlComments
	 */
	public String getMlComments() {
		return advisor.getUnrecognizedKey().name();
	}

	/**
	 * @return the rightToLeftRelationships
	 */
	public String getRightToLeftRelationships() {
		return advisor.getUnresolvedReference().name();
	}

	/**
	 * @return the selectorDefaultShouldAppearLast
	 */
	public String getSelectorDefaultShouldAppearLast() {
		return advisor.getWhitespaceInTag().name();
	}
}
