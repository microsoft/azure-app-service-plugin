/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;
import org.jenkinsci.plugins.microsoft.services.IAzureConnectionData;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

public class WebAppDeploymentContext implements Describable<WebAppDeploymentContext> {

    private final String azureCredentialsId;

    private String deploymentName;
    private String publishUrl;
    private String userName;
    private String passWord;
    private String resourceGroupName;
    private String appServicePlanName;
    private String webappName;
    private String skuName;
    private String skuCapacity;
    private String filePath;
    private String location;

    private static final String EMBEDDED_TEMPLATE_FILENAME = "/templateValue.json";

    @DataBoundConstructor
    public WebAppDeploymentContext(
            final String azureCredentialsId,
            final String resourceGroupName,
            final String appServicePlanName,
            final String webappName,
            final String skuName,
            final String skuCapacity,
            final String filePath,
            final String location) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroupName = resourceGroupName;
        this.appServicePlanName = appServicePlanName;
        this.webappName = webappName;
        this.skuName = skuName;
        this.skuCapacity = skuCapacity;
        this.filePath = filePath;
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<WebAppDeploymentContext> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public String getAzureCredentialsId() {
        return this.azureCredentialsId;
    }

    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    public String getAppServicePlanName() {
        return this.appServicePlanName;
    }

    public String getWebappName() {
        return this.webappName;
    }

    public String getSkuName() {
        return this.skuName;
    }

    public String getSkuCapacity() {
        return this.skuCapacity;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getLocation() {
        return this.location;
    }

    public Region getRegion() {
        return Region.fromName(location);
    }

    public void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getDeploymentName() {
        return this.deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getPublishUrl() {
        return this.publishUrl;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassWord() {
        return this.passWord;
    }

    public WebAppDeploymentCommandContext getCommandContext() {
        return new WebAppDeploymentCommandContext(
                AzureCredentials.getServicePrincipal(azureCredentialsId),
                resourceGroupName,
                location,
                webappName,
                appServicePlanName,
                filePath
        );
    }

    public String getEmbeddedTemplateName() {
        return EMBEDDED_TEMPLATE_FILENAME;
    }

    public void configureTemplate(JsonNode tmp) throws IllegalAccessException, AzureCloudException {
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.appServicePlanName, "appServicePlanName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.webappName, "webSiteName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.skuName, "skuName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("int", this.skuCapacity, "skuCapacity", null, tmp);
    }

    public IAzureConnectionData getAzureConnectionData() {
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends WebAppDeploymentContextDescriptor {

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return new StandardListBoxModel().withAll(CredentialsProvider.lookupCredentials(AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
        }
    }
}
