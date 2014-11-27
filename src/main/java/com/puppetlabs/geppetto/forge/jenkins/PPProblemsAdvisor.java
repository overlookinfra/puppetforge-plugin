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

import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.IPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.PotentialProblemsAdvisorBean;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

/**
 * A default implementation of IPotentialProblemsAdvisor that returns Warnings for all potential problems, and
 * Ignore for all stylistic problems
 */
public class PPProblemsAdvisor extends AbstractDescribableImpl<PPProblemsAdvisor> implements ExtensionPoint, Serializable {

	@Extension
	public static class ProblemsAdvisorDescriptor extends Descriptor<PPProblemsAdvisor> {
		public ListBoxModel doFillAssignmentToVarNamedStringItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillAssignmentToVarNamedTrustedItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillAttributeIsNotStringItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillBooleansInStringFormItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillCaseDefaultShouldAppearLastItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDeprecatedImportItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDeprecatedNodeInheritanceItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDeprecatedPlusEqualsItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDeprecatedVariableNameItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDqStringNotRequiredItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDqStringNotRequiredVariableItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillDuplicateParameterItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillEnsureShouldAppearFirstInResourceItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillInterpolatedNonBraceEnclosedHyphensItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillMissingDefaultInSelectorItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillMlCommentsItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillRightToLeftRelationshipsItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillSelectorDefaultShouldAppearLastItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillUnbracedInterpolationItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillUnquotedResourceTitlesItems() {
			return doFillValidationPreferenceItems();
		}

		public ListBoxModel doFillValidityAssertedAtRuntimeItems() {
			return doFillValidationPreferenceItems();
		}

		public PPProblemsAdvisor getDefaults() {
			return defaults;
		}

		@Override
		public String getDisplayName() {
			return "";
		}
	}

	private static final PPProblemsAdvisor defaults = new PPProblemsAdvisor();

	private static final long serialVersionUID = 1L;

	private final IPotentialProblemsAdvisor advisor;

	public PPProblemsAdvisor() {
		advisor = PuppetTarget.getDefault().getComplianceLevel().createValidationAdvisor(DefaultPotentialProblemsAdvisor.INSTANCE);
	}

	@DataBoundConstructor
	public PPProblemsAdvisor(ValidationPreference assignmentToVarNamedString, ValidationPreference assignmentToVarNamedTrusted,
			ValidationPreference attributeIsNotString, ValidationPreference booleansInStringForm,
			ValidationPreference caseDefaultShouldAppearLast, ValidationPreference deprecatedImport,
			ValidationPreference deprecatedNodeInheritance, ValidationPreference deprecatedPlusEquals,
			ValidationPreference deprecatedVariableName, ValidationPreference dqStringNotRequired,
			ValidationPreference dqStringNotRequiredVariable, ValidationPreference duplicateParameter,
			ValidationPreference ensureShouldAppearFirstInResource, ValidationPreference interpolatedNonBraceEnclosedHyphens,
			ValidationPreference missingDefaultInSelector, ValidationPreference mlComments, ValidationPreference rightToLeftRelationships,
			ValidationPreference selectorDefaultShouldAppearLast, ValidationPreference unbracedInterpolation,
			ValidationPreference unquotedResourceTitles, ValidationPreference validityAssertedAtRuntime) {
		this.advisor = new PotentialProblemsAdvisorBean(
			assignmentToVarNamedString, assignmentToVarNamedTrusted, attributeIsNotString, booleansInStringForm,
			caseDefaultShouldAppearLast, deprecatedImport, deprecatedNodeInheritance, deprecatedPlusEquals, deprecatedVariableName,
			dqStringNotRequired, dqStringNotRequiredVariable, duplicateParameter, ensureShouldAppearFirstInResource,
			interpolatedNonBraceEnclosedHyphens, missingDefaultInSelector, mlComments, selectorDefaultShouldAppearLast,
			unbracedInterpolation, unquotedResourceTitles, validityAssertedAtRuntime, rightToLeftRelationships);
	}

	public IPotentialProblemsAdvisor getAdvisor() {
		return advisor;
	}

	/**
	 * @return the assignmentToVarNamedString
	 */
	public String getAssignmentToVarNamedString() {
		return advisor.getAssignmentToVarNamedString().name();
	}

	/**
	 * @return the assignmentToVarNamedTrusted
	 */
	public String getAssignmentToVarNamedTrusted() {
		return advisor.getAssignmentToVarNamedTrusted().name();
	}

	/**
	 * @return the attributeIsNotString
	 */
	public String getAttributeIsNotString() {
		return advisor.getAttributeIsNotString().name();
	}

	/**
	 * @return the booleansInStringForm
	 */
	public String getBooleansInStringForm() {
		return advisor.getBooleansInStringForm().name();
	}

	/**
	 * @return the caseDefaultShouldAppearLast
	 */
	public String getCaseDefaultShouldAppearLast() {
		return advisor.getCaseDefaultShouldAppearLast().name();
	}

	/**
	 * @return the deprecatedImport
	 */
	public String getDeprecatedImport() {
		return advisor.getDeprecatedImport().name();
	}

	/**
	 * @return the deprecatedNodeInheritance
	 */
	public String getDeprecatedNodeInheritance() {
		return advisor.getDeprecatedNodeInheritance().name();
	}

	/**
	 * @return the deprecatedPlusEquals
	 */
	public String getDeprecatedPlusEquals() {
		return advisor.getDeprecatedPlusEquals().name();
	}

	/**
	 * @return the deprecatedVariableName
	 */
	public String getDeprecatedVariableName() {
		return advisor.getDeprecatedVariableName().name();
	}

	/**
	 * @return the dqStringNotRequired
	 */
	public String getDqStringNotRequired() {
		return advisor.getDqStringNotRequired().name();
	}

	/**
	 * @return the dqStringNotRequiredVariable
	 */
	public String getDqStringNotRequiredVariable() {
		return advisor.getDqStringNotRequiredVariable().name();
	}

	/**
	 * @return the duplicateParameter
	 */
	public String getDuplicateParameter() {
		return advisor.getDuplicateParameter().name();
	}

	/**
	 * @return the ensureShouldAppearFirstInResource
	 */
	public String getEnsureShouldAppearFirstInResource() {
		return advisor.getEnsureShouldAppearFirstInResource().name();
	}

	/**
	 * @return the interpolatedNonBraceEnclosedHyphens
	 */
	public String getInterpolatedNonBraceEnclosedHyphens() {
		return advisor.getInterpolatedNonBraceEnclosedHyphens().name();
	}

	/**
	 * @return the missingDefaultInSelector
	 */
	public String getMissingDefaultInSelector() {
		return advisor.getMissingDefaultInSelector().name();
	}

	/**
	 * @return the mlComments
	 */
	public String getMlComments() {
		return advisor.getMlComments().name();
	}

	/**
	 * @return the rightToLeftRelationships
	 */
	public String getRightToLeftRelationships() {
		return advisor.getRightToLeftRelationships().name();
	}

	/**
	 * @return the selectorDefaultShouldAppearLast
	 */
	public String getSelectorDefaultShouldAppearLast() {
		return advisor.getSelectorDefaultShouldAppearLast().name();
	}

	/**
	 * @return the unbracedInterpolation
	 */
	public String getUnbracedInterpolation() {
		return advisor.getUnbracedInterpolation().name();
	}

	/**
	 * @return the unquotedResourceTitles
	 */
	public String getUnquotedResourceTitles() {
		return advisor.getUnquotedResourceTitles().name();
	}

	/**
	 * @return the validityAssertedAtRuntime
	 */
	public String getValidityAssertedAtRuntime() {
		return advisor.getValidityAssertedAtRuntime().name();
	}
}
