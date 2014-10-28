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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.puppetlabs.geppetto.common.Strings;

abstract class ForgeBuilder {
	static final String FORGE_SERVICE_URL = "https://forgeapi.puppetlabs.com";

	private static final char[] ILLEGAL_GLOB_CHARS = new char[] { ',', '{', '}' };

	static FormValidation checkExcludeGlobs(String value) {
		StringTokenizer st = new StringTokenizer(value, "\r\n");
		while(st.hasMoreTokens()) {
			String glob = st.nextToken();
			for(char c : ILLEGAL_GLOB_CHARS)
				if(glob.indexOf(c) >= 0)
					return FormValidation.error("Glob cannot contain '%c'", c);
		}
		return FormValidation.ok();
	}

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

	public static String joinExcludeGlobs(Set<String> excludes) {
		if(excludes == null)
			return null;
		int top = excludes.size();
		if(top == 0)
			return "";
		List<String> sorted = Lists.newArrayList(excludes);
		Collections.sort(sorted);
		StringBuilder bld = new StringBuilder();
		bld.append(sorted.get(0));
		for(int idx = 1; idx < top; ++idx) {
			bld.append('\n');
			bld.append(sorted.get(idx));
		}
		return bld.toString();
	}

	static Set<String> parseExcludeGlobs(String value) {
		value = Strings.trimToNull(value);
		if(value == null)
			return Collections.emptySet();

		Set<String> patterns = Sets.newHashSet();
		StringTokenizer st = new StringTokenizer(value, "\r\n");
		while(st.hasMoreTokens())
			patterns.add(st.nextToken());
		return patterns;
	}
}
