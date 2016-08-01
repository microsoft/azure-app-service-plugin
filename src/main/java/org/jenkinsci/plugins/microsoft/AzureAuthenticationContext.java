package org.jenkinsci.plugins.microsoft;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;
import org.jenkinsci.plugins.microsoft.services.IAzureConnectionData;
import org.jenkinsci.plugins.microsoft.util.Constants;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public class AzureAuthenticationContext implements IAzureConnectionData, Describable<AzureAuthenticationContext> {
	
	private String subscriptionId;
	private String clientId;
	private String clientSecret;
	private String oauth2TokenEndpoint;
    
    private static final String EMBEDDED_TEMPLATE_FILENAME = "/templateValue.json";

    @DataBoundConstructor
	public AzureAuthenticationContext(
            final String subscriptionId,
            final String clientId,
            final String clientSecret,
            final String oauth2TokenEndpoint) {
	    this.subscriptionId = subscriptionId;
	    this.clientId = clientId;
	    this.clientSecret = clientSecret;
	    this.oauth2TokenEndpoint = oauth2TokenEndpoint;
	}
	
    @SuppressWarnings("unchecked")
	@Override
    public Descriptor<AzureAuthenticationContext>  getDescriptor() {
    	return Jenkins.getInstance().getDescriptor(getClass());
    }

	public String getSubscriptionId() {
		return this.subscriptionId;
	}
	
	public String getClientId() {
		return this.clientId;
	}
	
	public String getClientSecret() {
		return this.clientSecret;
	}
	
	public String getOauth2TokenEndpoint() {
		return this.oauth2TokenEndpoint;
	}
	
    @Extension
    public static final class DescriptorImpl extends Descriptor<AzureAuthenticationContext> {

    	public FormValidation doVerifyConfiguration(
                @QueryParameter String subscriptionId,
                @QueryParameter String clientId,
                @QueryParameter String clientSecret,
                @QueryParameter String oauth2TokenEndpoint) {
        			
            if (StringUtils.isBlank(subscriptionId)) {
                return FormValidation.error("Error: Subscription ID is missing");
            }
            if (StringUtils.isBlank(clientId)) {
                return FormValidation.error("Error: Native Client ID is missing");
            }
            if (StringUtils.isBlank(clientSecret)) {
                return FormValidation.error("Error: Azure Password is missing");
            }
            if (StringUtils.isBlank(oauth2TokenEndpoint)) {
                return FormValidation.error("Error: OAuth 2.0 Token Endpoint is missing");
            }
            
            String response = AzureManagementServiceDelegate.verifyConfiguration(
                    subscriptionId,
                    clientId,
                    clientSecret,
                    oauth2TokenEndpoint,
                    null);

            if (Constants.OP_SUCCESS.equalsIgnoreCase(response)) {
                return FormValidation.ok("Success");
            } else {
                return FormValidation.error(response);
            }            
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        	// Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        public String getDisplayName() {
            return null;
        }
    }
}
