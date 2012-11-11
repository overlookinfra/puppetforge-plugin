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

import static org.cloudsmith.geppetto.forge.impl.MetadataImpl.DEFAULT_EXCLUDES_PATTERN;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
import org.cloudsmith.geppetto.forge.v2.Forge;
import org.cloudsmith.geppetto.forge.v2.MetadataRepository;
import org.cloudsmith.geppetto.forge.v2.client.ForgePreferences;
import org.cloudsmith.geppetto.forge.v2.model.Dependency;
import org.cloudsmith.geppetto.forge.v2.model.Metadata;
import org.cloudsmith.geppetto.forge.v2.model.Module;
import org.cloudsmith.geppetto.forge.v2.model.Release;
import org.cloudsmith.geppetto.forge.v2.service.ReleaseService;
import org.cloudsmith.geppetto.pp.dsl.validation.DefaultPotentialProblemsAdvisor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.cloudsmith.hammer.puppet.validation.DetailedDiagnosticData;
import com.cloudsmith.hammer.puppet.validation.DiagnosticData;
import com.cloudsmith.hammer.puppet.validation.FileType;
import com.cloudsmith.hammer.puppet.validation.GraphHrefType;
import com.cloudsmith.hammer.puppet.validation.ModuleDependencyGraphOptions;
import com.cloudsmith.hammer.puppet.validation.ValidationFactory;
import com.cloudsmith.hammer.puppet.validation.ValidationOptions;
import com.cloudsmith.hammer.puppet.validation.ValidationServiceDiagnosticCode;
import com.cloudsmith.hammer.puppet.validation.runner.IEncodingProvider;
import com.cloudsmith.hammer.puppet.validation.runner.PPDiagnosticsRunner;
import com.google.gson.Gson;

public class ForgeValidator implements FileCallable<ResultWithDiagnostic<String>> {
	private static final long serialVersionUID = -2352185785743765350L;

	private ForgePreferences forgePreferences;

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static Diagnostic convertValidationDiagnostic(org.eclipse.emf.common.util.Diagnostic validationDiagnostic) {

		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSeverity(getSeverity(validationDiagnostic));
		diagnostic.setType(getDiagnosticType(validationDiagnostic));
		diagnostic.setMessage(validationDiagnostic.getMessage());
		diagnostic.setSource(validationDiagnostic.getSource());

		DiagnosticData data = (DiagnosticData) validationDiagnostic.getData().get(0);
		if(data instanceof DetailedDiagnosticData)
			diagnostic.setLocationLabel(locationLabel((DetailedDiagnosticData) data));

		return diagnostic;
	}

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

	private transient Forge forge;

	private transient File repositoryDir;

	private transient FileRepository localRepository;

	private static final Pattern GITHUB_REPO_URL_PATTERN = Pattern.compile("github.com[/:]([^/\\s]+)/([^/\\s]+)\\.git$");

	public ForgeValidator() {
	}

	public ForgeValidator(ForgePreferences forgePreferences) {
		this.forgePreferences = forgePreferences;
	}

	private File downloadAndInstall(ReleaseService releaseService, File modulesRoot,
			ResultWithDiagnostic<String> result, Release release) throws IOException {
		OpenBAStream content = new OpenBAStream();
		Module module = release.getModule();
		releaseService.download(module.getOwner().getUsername(), module.getName(), release.getVersion(), content);
		File moduleDir = new File(modulesRoot, module.getName());
		TarUtils.unpack(new GZIPInputStream(content.getInputStream()), moduleDir, false);
		return moduleDir;
	}

	private boolean findModuleFiles(File[] files, List<File> moduleFiles) throws InterruptedException, IOException {
		if(files != null) {
			int idx = files.length;
			while(--idx >= 0)
				if("Modulefile".equals(files[idx].getName()))
					return true;

			idx = files.length;
			while(--idx >= 0) {
				File file = files[idx];
				if(DEFAULT_EXCLUDES_PATTERN.matcher(file.getName()).matches())
					continue;

				if(findModuleFiles(file.listFiles(), moduleFiles))
					moduleFiles.add(file);
			}
		}
		return false;
	}

	private List<File> findModuleRoots() throws InterruptedException, IOException {
		// Scan for valid directories containing "Modulefile" files.

		List<File> moduleRoots = new ArrayList<File>();
		if(findModuleFiles(repositoryDir.listFiles(), moduleRoots))
			// The repository is a module in itself
			moduleRoots.add(repositoryDir);
		return moduleRoots;
	}

	private ModuleDependencyGraphOptions getDependencyGraphOptions(OutputStream dotStream, List<File> moduleLocations)
			throws IOException {
		// Assume service API
		ModuleDependencyGraphOptions graphOptions = ValidationFactory.eINSTANCE.createModuleDependencyGraphOptions();

		String repositoryURL = getRemoteURL();
		if(repositoryURL != null) {
			Matcher m = GITHUB_REPO_URL_PATTERN.matcher(repositoryURL);
			if(m.find()) {
				graphOptions.setGraphHrefPrefix(String.format(
					"https://github.com/%s/%s/blob/%s", m.group(1), m.group(2), getLocalRepository().getBranch()));
			}
		}
		graphOptions.setGraphHrefType(GraphHrefType.GITHUB);

		graphOptions.setDotStream(dotStream);
		graphOptions.setModulesToGraph(moduleLocations.toArray(new File[moduleLocations.size()]));

		graphOptions.setTitle("");
		return graphOptions;
	}

	private Forge getForge() {
		if(forge == null)
			forge = Forge.getInstance(forgePreferences);
		return forge;
	}

	private FileRepository getLocalRepository() throws IOException {
		if(localRepository == null) {
			File guessGitDir = new File(repositoryDir, ".git");
			boolean bare = !guessGitDir.isDirectory();
			FileRepositoryBuilder bld = new FileRepositoryBuilder();
			if(bare) {
				bld.setBare();
				bld.setGitDir(repositoryDir);
			}
			else
				bld.setWorkTree(repositoryDir);
			localRepository = bld.setup().build();
		}
		return localRepository;
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

	/**
	 * Obtains the remote URL that is referenced by the given <code>branchName</code>
	 * 
	 * @return The URL or <code>null</code> if it hasn't been configured
	 *         for the given branch.
	 */
	private String getRemoteURL() throws IOException {
		FileRepository repository = getLocalRepository();
		StoredConfig repoConfig = repository.getConfig();
		String configuredRemote = repoConfig.getString(
			ConfigConstants.CONFIG_BRANCH_SECTION, repository.getBranch(), ConfigConstants.CONFIG_KEY_REMOTE);
		return configuredRemote == null
				? null
				: repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, configuredRemote, ConfigConstants.CONFIG_KEY_URL);
	}

	private ValidationOptions getValidationOptions(List<File> moduleLocations, List<File> importedModuleLocations) {
		ValidationOptions options = ValidationFactory.eINSTANCE.createValidationOptions();
		options.setCheckLayout(true);
		options.setCheckModuleSemantics(true);
		options.setCheckReferences(true);
		options.setFileType(FileType.PUPPET_ROOT);
		options.setPlatformURI(PPDiagnosticsRunner.getPuppet_2_7_1());

		options.setEncodingProvider(new IEncodingProvider() {
			public String getEncoding(URI file) {
				return UTF_8.name();
			}
		});

		StringBuilder searchPath = new StringBuilder();

		searchPath.append(new java.io.File("lib", "*").getPath());

		for(File moduleLocation : moduleLocations)
			searchPath.append(":" + new File(moduleLocation, "*").getPath());

		for(File importedModuleLocation : importedModuleLocations)
			searchPath.append(":" + new File(importedModuleLocation, "*").getPath());

		options.setSearchPath(searchPath.toString());
		options.setProblemsAdvisor(new DefaultPotentialProblemsAdvisor());
		return options;
	}

	public ResultWithDiagnostic<String> invoke(File repositoryDir, VirtualChannel channel) throws IOException,
			InterruptedException {

		this.repositoryDir = repositoryDir;
		ResultWithDiagnostic<String> result = new ResultWithDiagnostic<String>();
		List<File> moduleRoots = findModuleRoots();
		if(moduleRoots.isEmpty()) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, "No modules found in repository"));
			return result;
		}

		List<Metadata> metadatas = new ArrayList<Metadata>();
		for(File moduleRoot : moduleRoots)
			metadatas.add(getModuleMetadata(moduleRoot));

		Set<Dependency> unresolvedCollector = new HashSet<Dependency>();
		Set<Release> releasesToDownload = resolveDependencies(metadatas, unresolvedCollector);
		for(Dependency unresolved : unresolvedCollector)
			result.addChild(new Diagnostic(Diagnostic.WARNING, "Unable to resolve dependency: " + unresolved));

		File importedModulesDir = new File(repositoryDir, "geppettoImportedModules");
		importedModulesDir.mkdirs();
		List<File> importedModuleRoots = new ArrayList<File>();

		ReleaseService releaseService = getForge().createReleaseService();
		for(Release release : releasesToDownload)
			importedModuleRoots.add(downloadAndInstall(releaseService, importedModulesDir, result, release));

		validate(moduleRoots, importedModuleRoots, result);

		return result;
	}

	private Set<Release> resolveDependencies(List<Metadata> metadatas, Set<Dependency> unresolvedCollector)
			throws IOException {
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
		MetadataRepository metadataRepo = getForge().createMetadataRepository();
		Set<Release> releasesToDownload = new HashSet<Release>();
		for(Dependency dep : deps)
			releasesToDownload.addAll(metadataRepo.deepResolve(dep, unresolvedCollector));
		return releasesToDownload;
	}

	private void validate(List<File> moduleLocations, List<File> importedModuleLocations,
			ResultWithDiagnostic<String> result) throws IOException {

		BasicDiagnostic diagnostics = new BasicDiagnostic();

		OpenBAStream dotStream = new OpenBAStream();

		ModuleDependencyGraphOptions graphOptions = getDependencyGraphOptions(dotStream, importedModuleLocations);
		ValidationOptions options = getValidationOptions(moduleLocations, importedModuleLocations);
		options.setDependencyGraphOptions(graphOptions);

		ValidationFactory.eINSTANCE.createValidationService().validate(
			diagnostics, repositoryDir, options, moduleLocations.toArray(new File[moduleLocations.size()]),
			new NullProgressMonitor());

		for(org.eclipse.emf.common.util.Diagnostic diagnostic : diagnostics.getChildren())
			result.addChild(convertValidationDiagnostic(diagnostic));

		if(result.getSeverity() != Diagnostic.ERROR)
			result.setResult(dotStream.toString(UTF_8));
	}
}
