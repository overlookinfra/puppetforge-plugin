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

import hudson.remoting.VirtualChannel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;

import com.google.inject.Module;
import com.puppetlabs.geppetto.common.os.FileUtils;
import com.puppetlabs.geppetto.common.os.StreamUtil.OpenBAStream;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.FileDiagnostic;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.graph.DependencyGraphProducer;
import com.puppetlabs.geppetto.graph.GithubURLHrefProducer;
import com.puppetlabs.geppetto.graph.ProgressMonitorCancelIndicator;
import com.puppetlabs.geppetto.graph.SVGProducer;
import com.puppetlabs.geppetto.graph.dependency.DependencyGraphModule;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Issue;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Option;
import com.puppetlabs.geppetto.puppetlint.PuppetLintService;
import com.puppetlabs.geppetto.ruby.RubyHelper;
import com.puppetlabs.geppetto.ruby.jrubyparser.JRubyServices;
import com.puppetlabs.geppetto.validation.FileType;
import com.puppetlabs.geppetto.validation.ValidationOptions;
import com.puppetlabs.geppetto.validation.ValidationService;
import com.puppetlabs.geppetto.validation.impl.ValidationModule;
import com.puppetlabs.geppetto.validation.runner.BuildResult;
import com.puppetlabs.geppetto.validation.runner.IEncodingProvider;
import com.puppetlabs.geppetto.validation.runner.PPDiagnosticsSetup;
import com.puppetlabs.graph.ICancel;

public class ForgeValidatorCallable extends ForgeServiceCallable<ResultWithDiagnostic<byte[]>> {
	private static final long serialVersionUID = -2352185785743765350L;

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final Pattern GITHUB_REPO_URL_PATTERN = Pattern.compile("github.com[/:]([^/\\s]+)/([^/\\s]+)\\.git$");

	private transient String repositoryHrefPrefix;

	static final String IMPORTED_MODULES_ROOT = "importedModules";

	private static int getSeverity(Issue issue) {
		switch(issue.getSeverity()) {
			case ERROR:
				return Diagnostic.ERROR;
			default:
				return Diagnostic.WARNING;
		}
	}

	private boolean checkModuleSemantics;

	private boolean checkReferences;

	private Option[] puppetLintOptions;

	private ComplianceLevel complianceLevel;

	public ForgeValidatorCallable() {
	}

	public ForgeValidatorCallable(String repositoryURL, String branchName, boolean checkReferences,
			boolean checkModuleSemantics, Option[] puppetLintOptions) {
		super(repositoryURL, branchName);
		if(complianceLevel == null)
			// TODO: Selectable in the UI
			complianceLevel = ComplianceLevel.PUPPET_2_7;
		this.checkReferences = checkReferences;
		this.checkModuleSemantics = checkModuleSemantics;
		this.puppetLintOptions = puppetLintOptions;
	}

	private void addGeppettoResult(Diagnostic geppettoDiag, byte[] svg, ResultWithDiagnostic<byte[]> result) {
		result.setResult(svg);
		for(Diagnostic child : geppettoDiag)
			if(child instanceof FileDiagnostic)
				result.addChild(convertFileDiagnostic((FileDiagnostic) child));
			else
				result.addChild(child);
	}

	@Override
	protected void addModules(Diagnostic diagnostic, List<Module> modules) {
		super.addModules(diagnostic, modules);
		modules.add(new ValidationModule());
		modules.add(new DependencyGraphModule(GithubURLHrefProducer.class, getRepositoryHrefPrefix()));
	}

	private Diagnostic convertFileDiagnostic(FileDiagnostic fd) {
		return new ValidationDiagnostic(
			fd.getSeverity(), fd.getType(), fd.getMessage(), repositoryHrefPrefix, getRelativePath(fd.getFile()),
			fd.getLineNumber());
	}

	private Diagnostic convertPuppetLintDiagnostic(File moduleRoot, Issue issue) {
		return new ValidationDiagnostic(
			getSeverity(issue), PuppetLintService.PUPPET_LINT, issue.getMessage(), repositoryHrefPrefix,
			getRelativePath(new File(moduleRoot, issue.getPath())), issue.getLineNumber());
	}

	private void geppettoValidation(Collection<File> moduleLocations, ResultWithDiagnostic<byte[]> result)
			throws IOException {

		Diagnostic geppettoDiag = new Diagnostic();
		Collection<File> importedModuleLocations = null;
		List<Metadata> metadatas = new ArrayList<Metadata>();
		for(File moduleRoot : moduleLocations) {
			Metadata md = getModuleMetadata(moduleRoot, geppettoDiag);
			if(md != null)
				metadatas.add(md);
		}

		if(geppettoDiag.getSeverity() == Diagnostic.ERROR) {
			addGeppettoResult(geppettoDiag, null, result);
			return;
		}

		if(checkModuleSemantics) {
			File importedModulesDir = new File(getBuildDir(), IMPORTED_MODULES_ROOT);
			importedModuleLocations = getForgeService(geppettoDiag).downloadDependencies(
				metadatas, importedModulesDir, geppettoDiag);
		}
		if(importedModuleLocations == null)
			importedModuleLocations = Collections.emptyList();

		RubyHelper.setRubyServicesFactory(JRubyServices.FACTORY);
		ValidationOptions options = getValidationOptions(moduleLocations, importedModuleLocations);
		new PPDiagnosticsSetup(complianceLevel, options.getProblemsAdvisor()).createInjectorAndDoEMFRegistration();

		BuildResult buildResult = getValidationService(geppettoDiag).validate(
			geppettoDiag, getRepositoryDir(), options,
			importedModuleLocations.toArray(new File[importedModuleLocations.size()]), new NullProgressMonitor());

		byte[] svg = null;
		if(checkModuleSemantics) {
			OpenBAStream dotStream = new OpenBAStream();
			ICancel cancel = new ProgressMonitorCancelIndicator(new NullProgressMonitor(), 1);
			getGraphProducer(geppettoDiag).produceGraph(
				cancel, "", moduleLocations.toArray(new File[moduleLocations.size()]), dotStream, buildResult, result);
			svg = produceSVG(dotStream.getInputStream(), geppettoDiag);
		}
		addGeppettoResult(geppettoDiag, svg, result);
	}

	public DependencyGraphProducer getGraphProducer(Diagnostic diag) {
		return getInjector(diag).getInstance(DependencyGraphProducer.class);
	}

	private String getRelativePath(File file) {
		IPath rootPath = Path.fromOSString(getRepositoryDir().getAbsolutePath());
		IPath relative;
		if(file.isAbsolute()) {
			IPath path = Path.fromOSString(file.getAbsolutePath());
			relative = path.makeRelativeTo(rootPath);
		}
		else
			relative = Path.fromOSString(file.getPath());
		return relative.toPortableString();
	}

	private synchronized String getRepositoryHrefPrefix() {
		if(repositoryHrefPrefix == null) {
			String repositoryURL = getRepositoryURL();
			Matcher m = GITHUB_REPO_URL_PATTERN.matcher(repositoryURL);
			if(m.find()) {
				repositoryHrefPrefix = String.format(
					"https://github.com/%s/%s/blob/%s", m.group(1), m.group(2), getBranchName());
				return repositoryHrefPrefix;
			}
			repositoryHrefPrefix = "";
		}
		return repositoryHrefPrefix.length() == 0
				? null
				: repositoryHrefPrefix;
	}

	private String getSearchPath(Collection<File> moduleLocations, Collection<File> importedModuleLocations) {
		StringBuilder searchPath = new StringBuilder();

		searchPath.append("lib/*:environments/$environment/*");

		for(File moduleLocation : moduleLocations)
			searchPath.append(":" + getRelativePath(moduleLocation) + "/*");

		for(File importedModuleLocation : importedModuleLocations)
			searchPath.append(":" + getRelativePath(importedModuleLocation) + "/*");
		return searchPath.toString();
	}

	public SVGProducer getSVGProducer(Diagnostic diag) {
		return getInjector(diag).getInstance(SVGProducer.class);
	}

	private ValidationOptions getValidationOptions(Collection<File> moduleLocations,
			Collection<File> importedModuleLocations) {
		ValidationOptions options = new ValidationOptions();
		options.setCheckLayout(true);
		options.setCheckModuleSemantics(checkModuleSemantics);
		options.setCheckReferences(checkReferences);

		if(moduleLocations.size() == 1 && getRepositoryDir().equals(moduleLocations.iterator().next()))
			options.setFileType(FileType.MODULE_ROOT);
		else
			options.setFileType(FileType.PUPPET_ROOT);

		options.setPlatformURI(PuppetTarget.forComplianceLevel(complianceLevel, false).getPlatformURI());
		options.setEncodingProvider(new IEncodingProvider() {
			public String getEncoding(URI file) {
				return UTF_8.name();
			}
		});

		options.setSearchPath(getSearchPath(moduleLocations, importedModuleLocations));
		options.setProblemsAdvisor(new DefaultPotentialProblemsAdvisor());
		return options;
	}

	ValidationService getValidationService(Diagnostic diag) {
		return getInjector(diag).getInstance(ValidationService.class);
	}

	@Override
	public ResultWithDiagnostic<byte[]> invoke(VirtualChannel channel) throws IOException, InterruptedException {

		ResultWithDiagnostic<byte[]> result = new ResultWithDiagnostic<byte[]>();
		FileUtils.rmR(getBuildDir());
		Collection<File> moduleRoots = findModuleRoots(result);
		if(moduleRoots.isEmpty()) {
			result.addChild(new Diagnostic(
				Diagnostic.ERROR, ValidationService.GEPPETTO, "No modules found in repository"));
			return result;
		}

		geppettoValidation(moduleRoots, result);
		if(puppetLintOptions != null)
			lintValidation(moduleRoots, result);
		return result;
	}

	private void lintValidation(Collection<File> moduleLocations, Diagnostic result) throws IOException {
		PuppetLintRunner runner = PuppetLintService.getInstance().getPuppetLintRunner();
		for(File moduleRoot : moduleLocations) {
			for(PuppetLintRunner.Issue issue : runner.run(moduleRoot, puppetLintOptions)) {
				Diagnostic diag = convertPuppetLintDiagnostic(moduleRoot, issue);
				if(diag != null)
					result.addChild(diag);
			}
		}
	}

	private byte[] produceSVG(InputStream dotStream, Diagnostic diag) throws IOException {
		ByteArrayOutputStream svgStream = new ByteArrayOutputStream();
		getSVGProducer(diag).produceSVG(dotStream, svgStream, false, new NullProgressMonitor());
		return svgStream.toByteArray();
	}
}