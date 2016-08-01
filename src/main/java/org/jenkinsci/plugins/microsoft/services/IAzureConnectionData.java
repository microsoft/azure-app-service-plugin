package org.jenkinsci.plugins.microsoft.services;

public interface IAzureConnectionData {
	public String getSubscriptionId();
	public String getClientId();
	public String getClientSecret();
	public String getOauth2TokenEndpoint();
}
