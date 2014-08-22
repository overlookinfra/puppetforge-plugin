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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.puppetlabs.geppetto.common.Strings;

abstract class ForgeBuilder {
	static FormValidation checkRelativePath(String value) {
		value = Strings.trimToNull(value);
		if(value != null) {
			if(value.indexOf('?') >= 0 || value.indexOf('*') >= 0)
				return FormValidation.error("Path cannot contain wildcard characters");
			IPath path = Path.fromOSString(value);
			if(path.isAbsolute())
				return FormValidation.error("Path must be relative");
			if(path.hasTrailingSeparator())
				return FormValidation.error("Path must not have a trailing separator");
			if(path.getDevice() != null)
				return FormValidation.error("Path must not have a device");
		}
		return FormValidation.ok();
	}

	static FormValidation checkURL(String value) {
		value = Strings.trimToNull(value);
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
