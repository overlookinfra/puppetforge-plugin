package org.cloudsmith.geppetto.forge.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.cloudsmith.geppetto.forge.v2.client.ForgePreferencesBean;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ForgeBuilder extends Builder {

	/**
	 * Descriptor for {@link ForgeBuilder}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See <tt>src/main/resources/org/cloudsmith/geppetto/jenkins/ForgeBuilder/*.jelly</tt> for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private String clientSecret;

		private String clientId;

		private String oauthURL;

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		/**
		 * Performs on-the-fly validation of the form field 'clientId'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckClientId(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'clientSecret'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckClientSecret(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		/**
		 * Performs on-the-fly validation of the form field 'oAuthURL'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckOAuthURL(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		/**
		 * @return the clientId
		 */
		public String getClientId() {
			return clientId;
		}

		/**
		 * @return the clientSecret
		 */
		public String getClientSecret() {
			return clientSecret;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return "Puppet Forge Publisher";
		}

		/**
		 * @return the authURL
		 */
		public String getOAuthURL() {
			return oauthURL;
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		/**
		 * @param clientId
		 *            the clientId to set
		 */
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
		 * @param clientSecret
		 *            the clientSecret to set
		 */
		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		/**
		 * @param oauthURL
		 *            the authURL to set
		 */
		public void setOAuthURL(String oauthURL) {
			this.oauthURL = oauthURL;
		}
	}

	private final String forgeOauthToken;

	private final String forgeLogin;

	private final String forgePassword;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ForgeBuilder(String forgeOauthToken, String forgeLogin, String forgePassword) {
		this.forgeOauthToken = forgeOauthToken;
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

		DescriptorImpl desc = getDescriptor();
		ForgePreferencesBean prefsBean = new ForgePreferencesBean();
		prefsBean.setOAuthAccessToken(forgeOauthToken);
		prefsBean.setLogin(forgeLogin);
		prefsBean.setPassword(forgePassword);
		prefsBean.setOAuthScopes("");
		prefsBean.setOAuthClientId(desc.getClientId());
		prefsBean.setOAuthClientSecret(desc.getClientSecret());
		prefsBean.setOAuthURL(desc.getOAuthURL());

		ResultWithDiagnostic<String> result = gitRoot.act(new ForgeValidator(prefsBean));
		return true;
	}
}
