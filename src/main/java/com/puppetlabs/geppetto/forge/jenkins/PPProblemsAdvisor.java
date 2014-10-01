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
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

/**
 * A default implementation of IPotentialProblemsAdvisor that returns Warnings for all potential problems, and
 * Ignore for all stylistic problems
 */
public class PPProblemsAdvisor extends AbstractDescribableImpl<PPProblemsAdvisor> implements ExtensionPoint, Serializable,
		IPotentialProblemsAdvisor {

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

	private static String string(ValidationPreference pref) {
		return pref == null
			? "IGNORE"
			: pref.name();
	}

	private static final PPProblemsAdvisor defaults = new PPProblemsAdvisor();

	private static final long serialVersionUID = 1L;

	private final ValidationPreference assignmentToVarNamedString;

	private final ValidationPreference assignmentToVarNamedTrusted;

	private final ValidationPreference attributeIsNotString;

	private final ValidationPreference booleansInStringForm;

	private final ValidationPreference caseDefaultShouldAppearLast;

	private final ValidationPreference deprecatedImport;

	private final ValidationPreference deprecatedNodeInheritance;

	private final ValidationPreference deprecatedPlusEquals;

	private final ValidationPreference deprecatedVariableName;

	private final ValidationPreference dqStringNotRequired;

	private final ValidationPreference dqStringNotRequiredVariable;

	private final ValidationPreference ensureShouldAppearFirstInResource;

	private final ValidationPreference interpolatedNonBraceEnclosedHyphens;

	private final ValidationPreference missingDefaultInSelector;

	private final ValidationPreference mlComments;

	private final ValidationPreference rightToLeftRelationships;

	private final ValidationPreference selectorDefaultShouldAppearLast;

	private final ValidationPreference unbracedInterpolation;

	private final ValidationPreference unquotedResourceTitles;

	private final ValidationPreference validityAssertedAtRuntime;

	public PPProblemsAdvisor() {
		this(PuppetTarget.getDefault().getComplianceLevel().createValidationAdvisor(new DefaultPotentialProblemsAdvisor()));
	}

	public PPProblemsAdvisor(IPotentialProblemsAdvisor ppa) {
		this.assignmentToVarNamedString = ppa.assignmentToVarNamedString();
		this.assignmentToVarNamedTrusted = ppa.assignmentToVarNamedTrusted();
		this.attributeIsNotString = ppa.attributeIsNotString();
		this.booleansInStringForm = ppa.booleansInStringForm();
		this.caseDefaultShouldAppearLast = ppa.caseDefaultShouldAppearLast();
		this.dqStringNotRequired = ppa.dqStringNotRequired();
		this.dqStringNotRequiredVariable = ppa.dqStringNotRequiredVariable();
		this.ensureShouldAppearFirstInResource = ppa.ensureShouldAppearFirstInResource();
		this.deprecatedImport = ppa.deprecatedImport();
		this.deprecatedNodeInheritance = ppa.deprecatedNodeInheritance();
		this.deprecatedPlusEquals = ppa.deprecatedPlusEquals();
		this.deprecatedVariableName = ppa.deprecatedVariableName();
		this.interpolatedNonBraceEnclosedHyphens = ppa.interpolatedNonBraceEnclosedHyphens();
		this.missingDefaultInSelector = ppa.missingDefaultInSelector();
		this.mlComments = ppa.mlComments();
		this.rightToLeftRelationships = ppa.rightToLeftRelationships();
		this.selectorDefaultShouldAppearLast = ppa.selectorDefaultShouldAppearLast();
		this.unbracedInterpolation = ppa.unbracedInterpolation();
		this.unquotedResourceTitles = ppa.unquotedResourceTitles();
		this.validityAssertedAtRuntime = ppa.validityAssertedAtRuntime();
	}

	@DataBoundConstructor
	public PPProblemsAdvisor(ValidationPreference assignmentToVarNamedString, ValidationPreference assignmentToVarNamedTrusted,
			ValidationPreference attributeIsNotString, ValidationPreference booleansInStringForm,
			ValidationPreference caseDefaultShouldAppearLast, ValidationPreference deprecatedImport,
			ValidationPreference deprecatedNodeInheritance, ValidationPreference deprecatedPlusEquals,
			ValidationPreference deprecatedVariableName, ValidationPreference dqStringNotRequired,
			ValidationPreference dqStringNotRequiredVariable, ValidationPreference ensureShouldAppearFirstInResource,
			ValidationPreference interpolatedNonBraceEnclosedHyphens, ValidationPreference missingDefaultInSelector,
			ValidationPreference mlComments, ValidationPreference rightToLeftRelationships,
			ValidationPreference selectorDefaultShouldAppearLast, ValidationPreference unbracedInterpolation,
			ValidationPreference unquotedResourceTitles, ValidationPreference validityAssertedAtRuntime) {
		this.assignmentToVarNamedString = assignmentToVarNamedString;
		this.assignmentToVarNamedTrusted = assignmentToVarNamedTrusted;
		this.attributeIsNotString = attributeIsNotString;
		this.booleansInStringForm = booleansInStringForm;
		this.caseDefaultShouldAppearLast = caseDefaultShouldAppearLast;
		this.deprecatedImport = deprecatedImport;
		this.deprecatedNodeInheritance = deprecatedNodeInheritance;
		this.deprecatedPlusEquals = deprecatedPlusEquals;
		this.deprecatedVariableName = deprecatedVariableName;
		this.dqStringNotRequired = dqStringNotRequired;
		this.dqStringNotRequiredVariable = dqStringNotRequiredVariable;
		this.ensureShouldAppearFirstInResource = ensureShouldAppearFirstInResource;
		this.interpolatedNonBraceEnclosedHyphens = interpolatedNonBraceEnclosedHyphens;
		this.missingDefaultInSelector = missingDefaultInSelector;
		this.mlComments = mlComments;
		this.rightToLeftRelationships = rightToLeftRelationships;
		this.selectorDefaultShouldAppearLast = selectorDefaultShouldAppearLast;
		this.unbracedInterpolation = unbracedInterpolation;
		this.unquotedResourceTitles = unquotedResourceTitles;
		this.validityAssertedAtRuntime = validityAssertedAtRuntime;
	}

	@Override
	public ValidationPreference assignmentToVarNamedString() {
		return assignmentToVarNamedString;
	}

	@Override
	public ValidationPreference assignmentToVarNamedTrusted() {
		return assignmentToVarNamedTrusted;
	}

	@Override
	public ValidationPreference attributeIsNotString() {
		return attributeIsNotString;
	}

	@Override
	public ValidationPreference booleansInStringForm() {
		return booleansInStringForm;
	}

	@Override
	public ValidationPreference caseDefaultShouldAppearLast() {
		return caseDefaultShouldAppearLast;
	}

	@Override
	public ValidationPreference deprecatedImport() {
		return deprecatedImport;
	}

	@Override
	public ValidationPreference deprecatedNodeInheritance() {
		return deprecatedNodeInheritance;
	}

	@Override
	public ValidationPreference deprecatedPlusEquals() {
		return deprecatedPlusEquals;
	}

	@Override
	public ValidationPreference deprecatedVariableName() {
		return deprecatedVariableName;
	}

	@Override
	public ValidationPreference dqStringNotRequired() {
		return dqStringNotRequired;
	}

	@Override
	public ValidationPreference dqStringNotRequiredVariable() {
		return dqStringNotRequiredVariable;
	}

	@Override
	public ValidationPreference ensureShouldAppearFirstInResource() {
		return ensureShouldAppearFirstInResource;
	}

	/**
	 * @return the assignmentToVarNamedString
	 */
	public String getAssignmentToVarNamedString() {
		return string(assignmentToVarNamedString);
	}

	/**
	 * @return the assignmentToVarNamedTrusted
	 */
	public String getAssignmentToVarNamedTrusted() {
		return string(assignmentToVarNamedTrusted);
	}

	/**
	 * @return the attributeIsNotString
	 */
	public String getAttributeIsNotString() {
		return string(attributeIsNotString);
	}

	/**
	 * @return the booleansInStringForm
	 */
	public String getBooleansInStringForm() {
		return string(booleansInStringForm);
	}

	/**
	 * @return the caseDefaultShouldAppearLast
	 */
	public String getCaseDefaultShouldAppearLast() {
		return string(caseDefaultShouldAppearLast);
	}

	/**
	 * @return the deprecatedImport
	 */
	public String getDeprecatedImport() {
		return string(deprecatedImport);
	}

	/**
	 * @return the deprecatedNodeInheritance
	 */
	public String getDeprecatedNodeInheritance() {
		return string(deprecatedNodeInheritance);
	}

	/**
	 * @return the deprecatedPlusEquals
	 */
	public String getDeprecatedPlusEquals() {
		return string(deprecatedPlusEquals);
	}

	/**
	 * @return the deprecatedVariableName
	 */
	public String getDprecatedVariableName() {
		return string(deprecatedVariableName);
	}

	/**
	 * @return the dqStringNotRequired
	 */
	public String getDqStringNotRequired() {
		return string(dqStringNotRequired);
	}

	/**
	 * @return the dqStringNotRequiredVariable
	 */
	public String getDqStringNotRequiredVariable() {
		return string(dqStringNotRequiredVariable);
	}

	/**
	 * @return the ensureShouldAppearFirstInResource
	 */
	public String getEnsureShouldAppearFirstInResource() {
		return string(ensureShouldAppearFirstInResource);
	}

	/**
	 * @return the interpolatedNonBraceEnclosedHyphens
	 */
	public String getInterpolatedNonBraceEnclosedHyphens() {
		return string(interpolatedNonBraceEnclosedHyphens);
	}

	/**
	 * @return the missingDefaultInSelector
	 */
	public String getMissingDefaultInSelector() {
		return string(missingDefaultInSelector);
	}

	/**
	 * @return the mlComments
	 */
	public String getMlComments() {
		return string(mlComments);
	}

	/**
	 * @return the rightToLeftRelationships
	 */
	public String getRightToLeftRelationships() {
		return string(rightToLeftRelationships);
	}

	/**
	 * @return the selectorDefaultShouldAppearLast
	 */
	public String getSelectorDefaultShouldAppearLast() {
		return string(selectorDefaultShouldAppearLast);
	}

	/**
	 * @return the unbracedInterpolation
	 */
	public String getUnbracedInterpolation() {
		return string(unbracedInterpolation);
	}

	/**
	 * @return the unquotedResourceTitles
	 */
	public String getUnquotedResourceTitles() {
		return string(unquotedResourceTitles);
	}

	/**
	 * @return the validityAssertedAtRuntime
	 */
	public String getValidityAssertedAtRuntime() {
		return string(validityAssertedAtRuntime);
	}

	@Override
	public ValidationPreference interpolatedNonBraceEnclosedHyphens() {
		return interpolatedNonBraceEnclosedHyphens;
	}

	@Override
	public ValidationPreference missingDefaultInSelector() {
		return missingDefaultInSelector;
	}

	@Override
	public ValidationPreference mlComments() {
		return mlComments;
	}

	@Override
	public ValidationPreference rightToLeftRelationships() {
		return rightToLeftRelationships;
	}

	@Override
	public ValidationPreference selectorDefaultShouldAppearLast() {
		return selectorDefaultShouldAppearLast;
	}

	@Override
	public ValidationPreference unbracedInterpolation() {
		return unbracedInterpolation;
	}

	@Override
	public ValidationPreference unquotedResourceTitles() {
		return unquotedResourceTitles;
	}

	@Override
	public ValidationPreference validityAssertedAtRuntime() {
		return validityAssertedAtRuntime;
	}

}
