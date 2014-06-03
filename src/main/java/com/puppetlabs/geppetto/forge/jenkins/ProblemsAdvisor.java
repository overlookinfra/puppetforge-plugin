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

import com.puppetlabs.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.IPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

/**
 * A default implementation of IPotentialProblemsAdvisor that returns Warnings for all potential problems, and
 * Ignore for all stylistic problems
 */
public class ProblemsAdvisor extends AbstractDescribableImpl<ProblemsAdvisor> implements ExtensionPoint, Serializable,
		IPotentialProblemsAdvisor {

	@Extension
	public static class ProblemsAdvisorDescriptor extends Descriptor<ProblemsAdvisor> {
		private final ProblemsAdvisor defaults = new ProblemsAdvisor(new DefaultPotentialProblemsAdvisor());

		public ListBoxModel doFillAssignmentToVarNamedStringItems() {
			return doFillValidationPreferenceItems(defaults.assignmentToVarNamedString());
		}

		public ListBoxModel doFillAssignmentToVarNamedTrustedItems() {
			return doFillValidationPreferenceItems(defaults.assignmentToVarNamedTrusted());
		}

		public ListBoxModel doFillBooleansInStringFormItems() {
			return doFillValidationPreferenceItems(defaults.booleansInStringForm());
		}

		public ListBoxModel doFillCaseDefaultShouldAppearLastItems() {
			return doFillValidationPreferenceItems(defaults.caseDefaultShouldAppearLast());
		}

		public ListBoxModel doFillCircularDependencyPreferenceItems() {
			return doFillValidationPreferenceItems(defaults.circularDependencyPreference());
		}

		public ListBoxModel doFillDqStringNotRequiredItems() {
			return doFillValidationPreferenceItems(defaults.dqStringNotRequired());
		}

		public ListBoxModel doFillDqStringNotRequiredVariableItems() {
			return doFillValidationPreferenceItems(defaults.dqStringNotRequiredVariable());
		}

		public ListBoxModel doFillEnsureShouldAppearFirstInResourceItems() {
			return doFillValidationPreferenceItems(defaults.ensureShouldAppearFirstInResource());
		}

		public ListBoxModel doFillInterpolatedNonBraceEnclosedHyphensItems() {
			return doFillValidationPreferenceItems(defaults.interpolatedNonBraceEnclosedHyphens());
		}

		public ListBoxModel doFillMissingDefaultInSelectorItems() {
			return doFillValidationPreferenceItems(defaults.missingDefaultInSelector());
		}

		public ListBoxModel doFillMlCommentsItems() {
			return doFillValidationPreferenceItems(defaults.mlComments());
		}

		public ListBoxModel doFillRightToLeftRelationshipsItems() {
			return doFillValidationPreferenceItems(defaults.rightToLeftRelationships());
		}

		public ListBoxModel doFillSelectorDefaultShouldAppearLastItems() {
			return doFillValidationPreferenceItems(defaults.selectorDefaultShouldAppearLast());
		}

		public ListBoxModel doFillUnbracedInterpolationItems() {
			return doFillValidationPreferenceItems(defaults.unbracedInterpolation());
		}

		public ListBoxModel doFillUnquotedResourceTitlesItems() {
			return doFillValidationPreferenceItems(defaults.unquotedResourceTitles());
		}

		public ListBoxModel doFillValidationPreferenceItems(ValidationPreference dflt) {
			List<Option> items = new ArrayList<Option>();
			for(ValidationPreference pref : ValidationPreference.values())
				items.add(new Option(pref.toString(), pref.name()));
			return new ListBoxModel(items);
		}

		public ProblemsAdvisor getDefaults() {
			return defaults;
		}

		@Override
		public String getDisplayName() {
			return "";
		}
	}

	private static final long serialVersionUID = 1L;

	private final ValidationPreference assignmentToVarNamedString;

	private final ValidationPreference assignmentToVarNamedTrusted;

	private final ValidationPreference booleansInStringForm;

	private final ValidationPreference caseDefaultShouldAppearLast;

	private final ValidationPreference circularDependencyPreference;

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

	public ProblemsAdvisor(IPotentialProblemsAdvisor a) {
		this.assignmentToVarNamedString = a.assignmentToVarNamedString();
		this.assignmentToVarNamedTrusted = a.assignmentToVarNamedTrusted();
		this.booleansInStringForm = a.booleansInStringForm();
		this.caseDefaultShouldAppearLast = a.caseDefaultShouldAppearLast();
		this.circularDependencyPreference = a.circularDependencyPreference();
		this.dqStringNotRequired = a.dqStringNotRequired();
		this.dqStringNotRequiredVariable = a.dqStringNotRequiredVariable();
		this.ensureShouldAppearFirstInResource = a.ensureShouldAppearFirstInResource();
		this.interpolatedNonBraceEnclosedHyphens = a.interpolatedNonBraceEnclosedHyphens();
		this.missingDefaultInSelector = a.missingDefaultInSelector();
		this.mlComments = a.mlComments();
		this.rightToLeftRelationships = a.rightToLeftRelationships();
		this.selectorDefaultShouldAppearLast = a.selectorDefaultShouldAppearLast();
		this.unbracedInterpolation = a.unbracedInterpolation();
		this.unquotedResourceTitles = a.unquotedResourceTitles();
	}

	@DataBoundConstructor
	public ProblemsAdvisor(ValidationPreference assignmentToVarNamedString,
			ValidationPreference assignmentToVarNamedTrusted, ValidationPreference booleansInStringForm,
			ValidationPreference caseDefaultShouldAppearLast, ValidationPreference circularDependencyPreference,
			ValidationPreference dqStringNotRequired, ValidationPreference dqStringNotRequiredVariable,
			ValidationPreference ensureShouldAppearFirstInResource,
			ValidationPreference interpolatedNonBraceEnclosedHyphens, ValidationPreference missingDefaultInSelector,
			ValidationPreference mlComments, ValidationPreference rightToLeftRelationships,
			ValidationPreference selectorDefaultShouldAppearLast, ValidationPreference unbracedInterpolation,
			ValidationPreference unquotedResourceTitles) {
		this.assignmentToVarNamedString = assignmentToVarNamedString;
		this.assignmentToVarNamedTrusted = assignmentToVarNamedTrusted;
		this.booleansInStringForm = booleansInStringForm;
		this.caseDefaultShouldAppearLast = caseDefaultShouldAppearLast;
		this.circularDependencyPreference = circularDependencyPreference;
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
	public ValidationPreference booleansInStringForm() {
		return booleansInStringForm;
	}

	@Override
	public ValidationPreference caseDefaultShouldAppearLast() {
		return caseDefaultShouldAppearLast;
	}

	@Override
	public ValidationPreference circularDependencyPreference() {
		return circularDependencyPreference;
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
		return assignmentToVarNamedString.name();
	}

	/**
	 * @return the assignmentToVarNamedTrusted
	 */
	public String getAssignmentToVarNamedTrusted() {
		return assignmentToVarNamedTrusted.name();
	}

	/**
	 * @return the booleansInStringForm
	 */
	public String getBooleansInStringForm() {
		return booleansInStringForm.name();
	}

	/**
	 * @return the caseDefaultShouldAppearLast
	 */
	public String getCaseDefaultShouldAppearLast() {
		return caseDefaultShouldAppearLast.name();
	}

	/**
	 * @return the circularDependencyPreference
	 */
	public String getCircularDependencyPreference() {
		return circularDependencyPreference.name();
	}

	/**
	 * @return the dqStringNotRequired
	 */
	public String getDqStringNotRequired() {
		return dqStringNotRequired.name();
	}

	/**
	 * @return the dqStringNotRequiredVariable
	 */
	public String getDqStringNotRequiredVariable() {
		return dqStringNotRequiredVariable.name();
	}

	/**
	 * @return the ensureShouldAppearFirstInResource
	 */
	public String getEnsureShouldAppearFirstInResource() {
		return ensureShouldAppearFirstInResource.name();
	}

	/**
	 * @return the interpolatedNonBraceEnclosedHyphens
	 */
	public String getInterpolatedNonBraceEnclosedHyphens() {
		return interpolatedNonBraceEnclosedHyphens.name();
	}

	/**
	 * @return the missingDefaultInSelector
	 */
	public String getMissingDefaultInSelector() {
		return missingDefaultInSelector.name();
	}

	/**
	 * @return the mlComments
	 */
	public String getMlComments() {
		return mlComments.name();
	}

	/**
	 * @return the rightToLeftRelationships
	 */
	public String getRightToLeftRelationships() {
		return rightToLeftRelationships.name();
	}

	/**
	 * @return the selectorDefaultShouldAppearLast
	 */
	public String getSelectorDefaultShouldAppearLast() {
		return selectorDefaultShouldAppearLast.name();
	}

	/**
	 * @return the unbracedInterpolation
	 */
	public String getUnbracedInterpolation() {
		return unbracedInterpolation.name();
	}

	/**
	 * @return the unquotedResourceTitles
	 */
	public String getUnquotedResourceTitles() {
		return unquotedResourceTitles.name();
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

}
