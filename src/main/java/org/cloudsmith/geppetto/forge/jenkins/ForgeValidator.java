package org.cloudsmith.geppetto.forge.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.cloudsmith.geppetto.forge.v2.client.ForgePreferencesBean;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ForgeValidator extends Builder {

	/**
	 * Descriptor for {@link ForgeValidator}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See <tt>src/main/resources/org/cloudsmith/geppetto/jenkins/ForgeValidator/*.jelly</tt> for the actual HTML fragment for the configuration
	 * screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private static FormValidation checkURL(String value) {
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

		private String clientId = "5017fd0247c2c027c8000001";

		private String clientSecret = "4142f3b56ac369f974267be05bd9d1e90927e940b5cac2b3f431d8a4a2ffd2e7";

		private String serviceURL = "http://localhost:4567";

		public DescriptorImpl() {
			super(ForgeValidator.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckClientId(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		public FormValidation doCheckClientSecret(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		public FormValidation doCheckOAuthURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		public FormValidation doCheckServiceURL(@QueryParameter String value) throws IOException, ServletException {
			return checkURL(value);
		}

		public String getClientId() {
			return clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return "Puppet Forge Validation";
		}

		public String getServiceURL() {
			return serviceURL;
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public void setServiceURL(String serviceURL) {
			this.serviceURL = serviceURL;
		}
	}

	/**
	 * Obtains the remote URL that is referenced by the given <code>branchName</code>
	 * 
	 * @return The URL or <code>null</code> if it hasn't been configured
	 *         for the given branch.
	 */
	public static String getRemoteURL(FileRepository repository) throws IOException {
		StoredConfig repoConfig = repository.getConfig();
		String configuredRemote = repoConfig.getString(
			ConfigConstants.CONFIG_BRANCH_SECTION, repository.getBranch(), ConfigConstants.CONFIG_KEY_REMOTE);
		return configuredRemote == null
				? null
				: repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, configuredRemote, ConfigConstants.CONFIG_KEY_URL);
	}

	private final String forgeOAuthToken;

	private final String forgeLogin;

	private final String forgePassword;

	public static final String ALREADY_PUBLISHED = "ALREADY_PUBLISHED";

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ForgeValidator(String forgeOAuthToken, String forgeLogin, String forgePassword) {
		this.forgeOAuthToken = forgeOAuthToken;
		this.forgeLogin = forgeLogin;
		this.forgePassword = forgePassword;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getForgeLogin() {
		return forgeLogin;
	}

	public String getForgeOAuthToken() {
		return forgeOAuthToken;
	}

	public String getForgePassword() {
		return forgePassword;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		SCM scm = build.getProject().getScm();
		if(!(scm instanceof GitSCM)) {
			listener.error("Unable to find Git SCM configuration in the project configuration");
			return false;
		}
		GitSCM git = (GitSCM) scm;
		FilePath gitRoot = git.getModuleRoot(build.getWorkspace(), build);
		List<UserRemoteConfig> repos = git.getUserRemoteConfigs();
		if(repos.size() == 0) {
			listener.error("Unable to find the Git repository URL");
			return false;
		}
		if(repos.size() > 1) {
			listener.error("Sorry, but publishing from multiple repositories is currently not supported");
			return false;
		}
		String repository = repos.get(0).getUrl();

		List<BranchSpec> branches = git.getBranches();
		String branchName = null;
		if(branches.size() == 0)
			branchName = Constants.MASTER;
		else if(branches.size() == 1) {
			BranchSpec branchSpec = branches.get(0);
			branchName = branchSpec.getName();
			if("**".equals(branchName))
				branchName = Constants.MASTER;
		}
		else {
			listener.error("Sorry, but publishing from multiple branches is not supported");
			return false;
		}

		DescriptorImpl desc = getDescriptor();
		ForgePreferencesBean prefsBean = new ForgePreferencesBean();
		prefsBean.setOAuthAccessToken(forgeOAuthToken);
		prefsBean.setLogin(forgeLogin);
		prefsBean.setPassword(forgePassword);
		prefsBean.setOAuthScopes("");
		prefsBean.setOAuthClientId(desc.getClientId());
		prefsBean.setOAuthClientSecret(desc.getClientSecret());

		String serviceURL = desc.getServiceURL();
		if(!serviceURL.endsWith("/"))
			serviceURL += "/";
		prefsBean.setOAuthURL(serviceURL + "oauth/token");
		prefsBean.setBaseURL(serviceURL + "v2/");

		boolean validationErrors = false;
		ResultWithDiagnostic<byte[]> result = gitRoot.act(new ForgeValidatorCallable(prefsBean, repository, branchName));

		// Emit non-validation diagnostics to the console
		Iterator<Diagnostic> diagIter = result.getChildren().iterator();
		List<Diagnostic> alreadyPublished = new ArrayList<Diagnostic>();
		while(diagIter.hasNext()) {
			Diagnostic diag = diagIter.next();
			if(ALREADY_PUBLISHED.equals(diag.getIssue())) {
				alreadyPublished.add(diag);
				diagIter.remove();
				continue;
			}

			if(diag.getResourcePath() == null) {
				switch(diag.getSeverity()) {
					case Diagnostic.ERROR:
						listener.error(diag.getMessage());
						break;
					case Diagnostic.FATAL:
						listener.fatalError(diag.getMessage());
						break;
					default:
						listener.getLogger().println(diag);
				}
				diagIter.remove();
			}
			else if(diag.getSeverity() == Diagnostic.ERROR)
				validationErrors = true;
		}

		if(validationErrors)
			listener.error("There are validation errors. See Validation Result for details");

		ValidationResult data = new ValidationResult(build, prefsBean, repository, branchName, alreadyPublished);
		build.addAction(data);
		data.setResult(result);
		return result.getSeverity() < Diagnostic.ERROR;
	}
}
