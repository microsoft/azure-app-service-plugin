package org.jenkinsci.plugins.microsoft.services;

import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import com.fasterxml.jackson.databind.JsonNode;

public interface IARMTemplateServiceData {
	public IAzureConnectionData getAzureConnectionData();
	public String getResourceGroupName();
	public String getEmbeddedTemplateName();
	public void configureTemplate(JsonNode tmp) throws IllegalAccessException, AzureCloudException;
}
