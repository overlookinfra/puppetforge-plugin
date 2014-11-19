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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puppetlabs.geppetto.forge.model.ForgeDocs;
import com.puppetlabs.geppetto.forge.model.ForgeResult;
import com.puppetlabs.geppetto.forge.model.VersionedName;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;

public class ValidationTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();

	private FreeStyleProject project;

	private int counter;

	private long start;

	public void assertDirectory(File dir) {
		assertTrue("must be able to create directory " + dir.getPath(), dir.isDirectory() || dir.mkdir());
	}

	public ForgeValidator getForgeValidator() {
		// @fmtOff
		try {
			return new ForgeValidator(
				"",
				"target/result.json",
				"target/docs.json",
				"https://forgestagingapi.puppetlabs.com",
				false,
				"",
				ComplianceLevel.PUPPET_2_6,
				ComplianceLevel.PUPPET_4_0,
				false,
				true,
				new PPProblemsAdvisor(),
				new ModuleValidationAdvisor(),
				ValidationPreference.IGNORE,
				"",
				ValidationImpact.DO_NOT_FAIL, false);
		}
		catch(FormValidation e) {
			fail(e.getMessage());
			return null;
		}
		// @fmtOn
	}

	public FreeStyleProject getProject() throws IOException {
		FreeStyleProject project = j.createFreeStyleProject("ValidationTest");
		DescribableList<Builder, Descriptor<Builder>> buildersList = project.getBuildersList();
		buildersList.add(new TestBuilder() {
			@Override
			public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,
					IOException {
				build.getWorkspace().deleteContents();
				return true;
			}
		});
		buildersList.add(new Shell("tar -xzf \"${RELEASE_TARBALL}\""));
		buildersList.add(getForgeValidator());
		return project;
	}

	public File getTarballFile(VersionedName releaseName) {
		StringBuilder bld = new StringBuilder();
		bld.append("/tarballs/");
		releaseName.toString(bld, '-');
		bld.append(".tar.gz");
		String tarballPath = bld.toString();
		URL tarballURL = getClass().getResource(tarballPath);
		assertNotNull("Unable to find tarball " + tarballPath, tarballURL);
		assertEquals("Tarball must be a file URL", "file", tarballURL.getProtocol());
		try {
			return new File(tarballURL.toURI());
		}
		catch(Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

	public File getTestOutput() {
		String basedirProp = System.getProperty("basedir");
		assertNotNull("basedir property must be set", basedirProp);
		File basedir = new File(basedirProp);
		assertTrue("basedir " + basedirProp + " must be a directory", basedir.isDirectory());
		File target = new File(basedir, "target");
		assertTrue("basedir " + basedirProp + " must have a target subdirectory", target.isDirectory());
		File testOutput = new File(target, "test-output");
		assertDirectory(testOutput);
		return testOutput;
	}

	@Test
	public void handleBadRubyDesc() throws Exception {
		validateTarball(getTarballFile(new VersionedName("leoc", "phpmyadmin", "0.0.1")));
	}

	@Test
	public void handleCastError() throws Exception {
		validateTarball(getTarballFile(new VersionedName("maestrodev", "maestro_demo", "1.0.1")));
	}

	@Test
	public void handleConvertDiagnosticChildrenError() throws Exception {
		validateTarball(getTarballFile(new VersionedName("maestrodev", "rvm", "1.7.0")));
	}

	@Test
	public void handleLinksAppointingParentDirectory() throws Exception {
		validateTarball(getTarballFile(new VersionedName("emyl", "vagrant", "0.1.0")));
	}

	@Test
	public void handleModuleNameWithDashAndSlash() throws Exception {
		validateTarball(getTarballFile(new VersionedName("mcanevet", "hostapd", "0.2.0")));
	}

	@Test
	public void handleNoTypesOrFunctions() throws Exception {
		validateTarball(getTarballFile(new VersionedName("maestrodev", "jetty", "1.1.2")));
	}

	// @Test
	public void memoryLeak() throws IOException {
		final PathMatcher glob = FileSystems.getDefault().getPathMatcher("regex:.*\\.tar\\.gz");
		Files.walkFileTree(
			Paths.get("/home/thhal/sonatype-work/nexus/storage/puppetforge/com/puppetlabs/forge"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
					if(attr.isRegularFile() && glob.matches(file))
						validateTarball(file.toFile());
					return FileVisitResult.CONTINUE;
				}
			});
	}

	@Before
	public void setUp() throws IOException {
		project = getProject();
		start = System.currentTimeMillis();
	}

	public void validateTarball(File tarball) throws IOException {
		Action paramsAction = new ParametersAction(new StringParameterValue("RELEASE_TARBALL", tarball.getAbsolutePath()));
		long start = System.currentTimeMillis();
		Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause(), paramsAction);
		try {
			while(!future.isDone()) {
				if(future.isCancelled()) {
					fail("Build was cancelled");
					return;
				}
				Thread.sleep(100);
			}
			FreeStyleBuild build = future.get();
			assertTrue("Build should complete", build.getResult().isCompleteBuild());
			try (InputStream in = new BufferedInputStream(build.getWorkspace().child("target/result.json").read())) {
				ObjectMapper mapper = ForgeValidator.getMapper();
				ForgeResult result = mapper.readValue(in, ForgeResult.class);
				assertEquals("Should provide result for all versions", ComplianceLevel.values().length, result.getResults().size());
			}
			catch(FileNotFoundException e) {
				StringBuilder bld = new StringBuilder();
				try (Reader logReder = build.getLogReader()) {
					char[] buf = new char[1024];
					int cnt = 0;
					while((cnt = logReder.read(buf)) > 0)
						bld.append(buf, 0, cnt);
				}
				fail(bld.toString());
			}
			try (InputStream in = new BufferedInputStream(build.getWorkspace().child("target/docs.json").read())) {
				ObjectMapper mapper = ForgeValidator.getMapper();
				ForgeDocs docs = mapper.readValue(in, ForgeDocs.class);
				assertNotNull("Should contain a release slug", docs.getRelease());
				assertNotNull("Should always produce a types collection", docs.getTypes());
				assertNotNull("Should always produce a functions collection", docs.getFunctions());
			}
			catch(FileNotFoundException e) {
				fail("Should emit a docs.json file");
			}
			long now = System.currentTimeMillis();
			long duration = now - start;
			long avgDuration = (start - this.start) / ++counter;
			String n = tarball.getName();
			System.out.format("%-30s %6d ms, avg %6d, count %4d%n", n.substring(0, n.length() - 7), duration, avgDuration, counter);
		}
		catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if(msg == null)
				msg = e.getClass().getName();
			fail(msg);
		}
	}
}
