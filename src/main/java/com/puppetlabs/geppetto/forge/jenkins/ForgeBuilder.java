/**
 * Copyright (c) 2014 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.forge.jenkins;

import hudson.util.FormValidation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

abstract class ForgeBuilder {
	static FormValidation checkURL(String value) {
		if(value == null)
			return FormValidation.ok();
		try {
			URI uri = new URI(value);
			if(!uri.isAbsolute())
				return FormValidation.error("URL must be absolute");

			if(uri.isOpaque())
				return FormValidation.error("URL must not be opaque");

			uri.toURL();
			return FormValidation.ok();
		}
		catch(MalformedURLException e) {
			return FormValidation.error(e, "Not a valid URL");
		}
		catch(URISyntaxException e) {
			return FormValidation.error(e, "Not a valid URL");
		}
	}

	static final String FORGE_SERVICE_URL = "https://forgeapi.puppetlabs.com";

	public static final String ALREADY_PUBLISHED = "ALREADY_PUBLISHED";
}
