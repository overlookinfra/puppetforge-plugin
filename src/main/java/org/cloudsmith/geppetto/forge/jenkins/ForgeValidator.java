/**
 * Copyright (c) 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Cloudsmith
 * 
 */
package org.cloudsmith.geppetto.forge.jenkins;

import hudson.remoting.VirtualChannel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.cloudsmith.geppetto.common.os.StreamUtil;
import org.cloudsmith.geppetto.common.os.StreamUtil.OpenBAStream;
import org.cloudsmith.geppetto.forge.ForgeFactory;
import org.cloudsmith.geppetto.forge.util.JsonUtils;
import org.cloudsmith.geppetto.forge.util.TarUtils;
import org.cloudsmith.geppetto.forge.v2.MetadataRepository;
import org.cloudsmith.geppetto.forge.v2.client.ForgePreferences;
import org.cloudsmith.geppetto.forge.v2.model.Dependency;
import org.cloudsmith.geppetto.forge.v2.model.Metadata;
import org.cloudsmith.geppetto.forge.v2.model.Module;
import org.cloudsmith.geppetto.forge.v2.model.Release;
import org.cloudsmith.geppetto.forge.v2.service.ReleaseService;
import org.cloudsmith.geppetto.pp.dsl.PPStandaloneSetup;
import org.cloudsmith.geppetto.pp.dsl.target.PptpResourceUtil;
import org.cloudsmith.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import org.cloudsmith.geppetto.puppetlint.PuppetLintRunner;
import org.cloudsmith.geppetto.puppetlint.PuppetLintRunner.Issue;
import org.cloudsmith.geppetto.puppetlint.PuppetLintService;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.URI;

import com.cloudsmith.hammer.puppet.validation.DetailedDiagnosticData;
import com.cloudsmith.hammer.puppet.validation.FileType;
import com.cloudsmith.hammer.puppet.validation.GraphHrefType;
import com.cloudsmith.hammer.puppet.validation.ModuleDependencyGraphOptions;
import com.cloudsmith.hammer.puppet.validation.ValidationFactory;
import com.cloudsmith.hammer.puppet.validation.ValidationOptions;
import com.cloudsmith.hammer.puppet.validation.ValidationServiceDiagnosticCode;
import com.cloudsmith.hammer.puppet.validation.graphs.SVGProducer;
import com.cloudsmith.hammer.puppet.validation.runner.IEncodingProvider;
import com.google.gson.Gson;

public class ForgeValidator extends ForgeCallable<ResultWithDiagnostic<byte[]>> {
	private static final long serialVersionUID = -2352185785743765350L;

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final Pattern GITHUB_REPO_URL_PATTERN = Pattern.compile("github.com[/:]([^/\\s]+)/([^/\\s]+)\\.git$");

	private static DiagnosticType getDiagnosticType(org.eclipse.emf.common.util.Diagnostic validationDiagnostic) {
		DiagnosticType type;
		switch(validationDiagnostic.getCode()) {
			case ValidationServiceDiagnosticCode.CATALOG_PARSER_VALUE:
				type = DiagnosticType.CATALOG_PARSER;
				break;
			case ValidationServiceDiagnosticCode.CATALOG_VALUE:
				type = DiagnosticType.CATALOG;
				break;
			case ValidationServiceDiagnosticCode.FORGE_VALUE:
				type = DiagnosticType.FORGE;
				break;
			case ValidationServiceDiagnosticCode.GEPPETTO_SYNTAX_VALUE:
				type = DiagnosticType.GEPPETTO_SYNTAX;
				break;
			case ValidationServiceDiagnosticCode.GEPPETTO_VALUE:
				type = DiagnosticType.GEPPETTO;
				break;
			case ValidationServiceDiagnosticCode.INTERNAL_ERROR_VALUE:
				type = DiagnosticType.INTERNAL_ERROR;
				break;
			case ValidationServiceDiagnosticCode.PUPPET_LINT_VALUE:
				type = DiagnosticType.PUPPET_LINT;
				break;
			case ValidationServiceDiagnosticCode.RUBY_SYNTAX_VALUE:
				type = DiagnosticType.RUBY_SYNTAX;
				break;
			case ValidationServiceDiagnosticCode.RUBY_VALUE:
				type = DiagnosticType.RUBY;
				break;
			default:
				type = DiagnosticType.UNKNOWN;
		}
		return type;
	}

	private static int getSeverity(Issue issue) {
		switch(issue.getSeverity()) {
			case ERROR:
				return Diagnostic.ERROR;
			default:
				return Diagnostic.WARNING;
		}
	}

	private static int getSeverity(org.eclipse.emf.common.util.Diagnostic validationDiagnostic) {
		int severity;
		switch(validationDiagnostic.getSeverity()) {
			case org.eclipse.emf.common.util.Diagnostic.ERROR:
				severity = Diagnostic.ERROR;
				break;
			case org.eclipse.emf.common.util.Diagnostic.WARNING:
				severity = Diagnostic.WARNING;
				break;
			case org.eclipse.emf.common.util.Diagnostic.INFO:
				severity = Diagnostic.INFO;
				break;
			default:
				severity = Diagnostic.OK;
		}
		return severity;
	}

	private static String locationLabel(DetailedDiagnosticData detail) {
		int lineNumber = detail.getLineNumber();
		int offset = detail.getOffset();
		int length = detail.getLength();
		StringBuilder builder = new StringBuilder();
		if(lineNumber > 0)
			builder.append(lineNumber);
		else
			builder.append("-");

		if(offset >= 0) {
			builder.append("(");
			builder.append(offset);
			if(length >= 0) {
				builder.append(",");
				builder.append(length);
			}
			builder.append(")");
		}
		return builder.toString();
	}

	private transient String repositoryHrefPrefix;

	public ForgeValidator() {
	}

	public ForgeValidator(ForgePreferences forgePreferences, String repositoryURL, String branchName) {
		super(forgePreferences, repositoryURL, branchName);
	}

	private Diagnostic convertPuppetLintDiagnostic(File moduleRoot, Issue issue) {
		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSeverity(getSeverity(issue));
		diagnostic.setMessage(issue.getMessage());
		diagnostic.setType(DiagnosticType.PUPPET_LINT);
		diagnostic.setSource(getRepositoryHrefPrefix());
		diagnostic.setResourcePath(getRelativePath(new File(moduleRoot, issue.getPath())));
		diagnostic.setLocationLabel(Integer.toString(issue.getLineNumber()));
		return diagnostic;
	}

	private Diagnostic convertValidationDiagnostic(org.eclipse.emf.common.util.Diagnostic validationDiagnostic) {

		Object dataObj = validationDiagnostic.getData().get(0);
		String resourcePath = null;
		String locationLabel = null;
		if(dataObj instanceof DetailedDiagnosticData) {
			DetailedDiagnosticData details = (DetailedDiagnosticData) dataObj;
			resourcePath = details.getFile().getPath();
			if(resourcePath != null && resourcePath.startsWith(BUILD_DIR))
				// We don't care about warnings/errors from imported modules
				return null;
			locationLabel = locationLabel(details);
		}

		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSeverity(getSeverity(validationDiagnostic));
		diagnostic.setType(getDiagnosticType(validationDiagnostic));
		diagnostic.setMessage(validationDiagnostic.getMessage());
		diagnostic.setSource(getRepositoryHrefPrefix());
		diagnostic.setResourcePath(resourcePath);
		diagnostic.setLocationLabel(locationLabel);
		return diagnostic;
	}

	private File downloadAndInstall(ReleaseService releaseService, File modulesRoot,
			ResultWithDiagnostic<byte[]> result, Release release) throws IOException {
		OpenBAStream content = new OpenBAStream();
		Module module = release.getModule();
		releaseService.download(module.getOwner().getUsername(), module.getName(), release.getVersion(), content);
		File moduleDir = new File(modulesRoot, module.getName());
		TarUtils.unpack(new GZIPInputStream(content.getInputStream()), moduleDir, false);
		return moduleDir;
	}

	private void geppettoValidation(List<File> moduleLocations, List<File> importedModuleLocations,
			ResultWithDiagnostic<byte[]> result) throws IOException {

		BasicDiagnostic diagnostics = new BasicDiagnostic();

		OpenBAStream dotStream = new OpenBAStream();

		ModuleDependencyGraphOptions graphOptions = getDependencyGraphOptions(dotStream, importedModuleLocations);
		ValidationOptions options = getValidationOptions(moduleLocations, importedModuleLocations);
		options.setDependencyGraphOptions(graphOptions);

		ValidationFactory.eINSTANCE.createValidationService().validate(
			diagnostics, getRepositoryDir(), options,
			importedModuleLocations.toArray(new File[importedModuleLocations.size()]), new NullProgressMonitor());

		for(org.eclipse.emf.common.util.Diagnostic diagnostic : diagnostics.getChildren()) {
			Diagnostic diag = convertValidationDiagnostic(diagnostic);
			if(diag != null)
				result.addChild(diag);
		}
		result.setResult(produceSVG(dotStream.getInputStream()));
	}

	private ModuleDependencyGraphOptions getDependencyGraphOptions(OutputStream dotStream, List<File> moduleLocations)
			throws IOException {
		// Assume service API
		ModuleDependencyGraphOptions graphOptions = ValidationFactory.eINSTANCE.createModuleDependencyGraphOptions();

		String hrefPrefix = getRepositoryHrefPrefix();
		if(hrefPrefix != null) {
			graphOptions.setGraphHrefPrefix(hrefPrefix);
			graphOptions.setGraphHrefType(GraphHrefType.GITHUB);
		}

		graphOptions.setDotStream(dotStream);
		graphOptions.setModulesToGraph(moduleLocations.toArray(new File[moduleLocations.size()]));

		graphOptions.setTitle("");
		return graphOptions;
	}

	private Metadata getModuleMetadata(File moduleDirectory) throws IOException {
		StringWriter writer = new StringWriter();
		try {
			Gson gson = JsonUtils.getGSon();
			gson.toJson(ForgeFactory.eINSTANCE.createForgeService().loadModule(moduleDirectory), writer);
		}
		finally {
			StreamUtil.close(writer);
		}
		Gson gson = getForge().createGson();
		return gson.fromJson(writer.toString(), Metadata.class);
	}

	private String getRelativePath(File file) {
		IPath rootPath = Path.fromOSString(getRepositoryDir().getAbsolutePath());
		IPath path = Path.fromOSString(file.getAbsolutePath());
		IPath relative = path.makeRelativeTo(rootPath);
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

	private ValidationOptions getValidationOptions(List<File> moduleLocations, List<File> importedModuleLocations) {
		ValidationOptions options = ValidationFactory.eINSTANCE.createValidationOptions();
		options.setCheckLayout(true);
		options.setCheckModuleSemantics(true);
		options.setCheckReferences(true);
		options.setFileType(FileType.PUPPET_ROOT);

		// TODO: Selectable in the UI
		options.setPlatformURI(PptpResourceUtil.getPuppet_2_7_19());

		options.setEncodingProvider(new IEncodingProvider() {
			public String getEncoding(URI file) {
				return UTF_8.name();
			}
		});

		StringBuilder searchPath = new StringBuilder();

		searchPath.append("lib/*:environments/$environment/*");

		for(File moduleLocation : moduleLocations)
			searchPath.append(":" + getRelativePath(moduleLocation) + "/*");

		for(File importedModuleLocation : importedModuleLocations)
			searchPath.append(":" + getRelativePath(importedModuleLocation) + "/*");

		options.setSearchPath(searchPath.toString());
		options.setProblemsAdvisor(new DefaultPotentialProblemsAdvisor());
		return options;
	}

	@Override
	public ResultWithDiagnostic<byte[]> invoke(VirtualChannel channel) throws IOException, InterruptedException {

		ResultWithDiagnostic<byte[]> result = new ResultWithDiagnostic<byte[]>();
		List<File> moduleRoots = findModuleRoots();
		if(moduleRoots.isEmpty()) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, "No modules found in repository"));
			return result;
		}

		PPStandaloneSetup.doSetup();
		MetadataRepository metadataRepo = getForge().createMetadataRepository();

		int alreadyPublishedCount = 0;
		List<Metadata> metadatas = new ArrayList<Metadata>();
		for(File moduleRoot : moduleRoots) {
			Metadata metadata = getModuleMetadata(moduleRoot);
			if(metadataRepo.resolve(metadata.getName(), metadata.getVersion()) != null) {
				Diagnostic diag = new Diagnostic(Diagnostic.WARNING, "Module " + metadata.getName() + ':' +
						metadata.getVersion() + " has already been published");
				diag.setIssue(ForgeBuilder.ALREADY_PUBLISHED);
				diag.setResourcePath(moduleRoot.getAbsolutePath());
				result.addChild(diag);
				++alreadyPublishedCount;
			}
			metadatas.add(metadata);
		}

		if(metadatas.size() == alreadyPublishedCount) {
			// This is an error but we still continue and produce the validation and graph
			result.addChild(new Diagnostic(
				Diagnostic.ERROR, "All modules have already been published at their current version"));
		}

		Set<Dependency> unresolvedCollector = new HashSet<Dependency>();
		Set<Release> releasesToDownload = resolveDependencies(metadataRepo, metadatas, unresolvedCollector);
		for(Dependency unresolved : unresolvedCollector)
			result.addChild(new Diagnostic(Diagnostic.WARNING, String.format(
				"Unable to resolve dependency: %s:%s", unresolved.getName(),
				unresolved.getVersionRequirement().toString())));

		List<File> importedModuleRoots;
		if(!releasesToDownload.isEmpty()) {
			File importedModulesDir = new File(getBuildDir(), IMPORTED_MODULES_ROOT);
			importedModulesDir.mkdirs();
			importedModuleRoots = new ArrayList<File>();

			ReleaseService releaseService = getForge().createReleaseService();
			for(Release release : releasesToDownload) {
				result.addChild(new Diagnostic(Diagnostic.INFO, "Installing dependent module " + release.getFullName() +
						':' + release.getVersion()));
				importedModuleRoots.add(downloadAndInstall(releaseService, importedModulesDir, result, release));
			}
		}
		else {
			importedModuleRoots = Collections.emptyList();
			if(unresolvedCollector.isEmpty())
				result.addChild(new Diagnostic(Diagnostic.INFO, "No addtional dependencies were detected"));
		}

		geppettoValidation(moduleRoots, importedModuleRoots, result);
		lintValidation(moduleRoots, result);
		return result;
	}

	private void lintValidation(List<File> moduleLocations, Diagnostic result) throws IOException {
		PuppetLintRunner runner = PuppetLintService.getInstance().getPuppetLintRunner();
		for(File moduleRoot : moduleLocations) {
			for(PuppetLintRunner.Issue issue : runner.run(moduleRoot)) {
				Diagnostic diag = convertPuppetLintDiagnostic(moduleRoot, issue);
				if(diag != null)
					result.addChild(diag);
			}
		}
	}

	private byte[] produceSVG(InputStream dotStream) throws IOException {
		ByteArrayOutputStream svgStream = new ByteArrayOutputStream();
		new SVGProducer().produceSVG(dotStream, svgStream, false, new NullProgressMonitor());
		return svgStream.toByteArray();
	}

	private Set<Release> resolveDependencies(MetadataRepository metadataRepo, List<Metadata> metadatas,
			Set<Dependency> unresolvedCollector) throws IOException {
		// Resolve missing dependencies
		Set<Dependency> deps = new HashSet<Dependency>();
		for(Metadata metadata : metadatas)
			deps.addAll(metadata.getDependencies());

		// Remove the dependencies that appoints modules that we have in the workspace
		Iterator<Dependency> depsItor = deps.iterator();
		nextDep: while(depsItor.hasNext()) {
			Dependency dep = depsItor.next();
			for(Metadata metadata : metadatas)
				if(dep.matches(metadata)) {
					depsItor.remove();
					continue nextDep;
				}
		}

		// Resolve remaining dependencies
		Set<Release> releasesToDownload = new HashSet<Release>();
		for(Dependency dep : deps)
			releasesToDownload.addAll(metadataRepo.deepResolve(dep, unresolvedCollector));
		return releasesToDownload;
	}
}
