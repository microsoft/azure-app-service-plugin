/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;

import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

public class ValidateWebAppCommand implements ICommand<ValidateWebAppCommand.IValidateWebAppCommandData> {

    @Override
    public void execute(ValidateWebAppCommand.IValidateWebAppCommandData context) {
        try {
            final String resourceGroupName = context.getResourceGroupName();
            final Azure azureClient = TokenCache.getInstance(context.getAzureServicePrincipal()).getAzureClient();

            String webappname = context.getAppServiceName();
            context.logStatus(String.format("Checking if the Azure Webapp with name '%s' exist.", resourceGroupName));

            final WebApp app = azureClient.webApps().getByGroup(resourceGroupName, webappname);
            if (app != null) {
                context.logStatus(String.format("Azure Webapp '%s' found.", resourceGroupName));
                context.setDeploymentState(DeploymentState.Success);
            } else {
                context.logStatus(
                        String.format("Azure Webapp '%s' not found.", resourceGroupName));
                context.setDeploymentState(DeploymentState.UnSuccessful);
            }

        } catch (Exception e) {
            context.logError("Error validating the Azure Web App:", e);
        }

        /*String resourceGroupName = context.getResourceGroupName();
        try {
            String webappname = context.getWebappName();
            boolean found = false;
            context.logStatus(
                    String.format("Checking if the Azure Webapp with name '%s' exist.", resourceGroupName));
            ResourceManagementClient rmc = context.getResourceClient();
            WebSiteManagementClient website = context.getWebsiteClient();
            Site site = website.getSitesOperations().getSite(resourceGroupName, webappname, null).getBody();
            context.logStatus(
                    String.format("Azure Webapp '%s' found.", resourceGroupName));
            context.setDeploymentState(DeploymentState.Success);
        } catch (IOException | CloudException | IllegalArgumentException e) {
            if (CloudException.class.isInstance(e)) {
                if (((CloudException) e).getResponse().code() == 404) {
                    context.logStatus(
                            String.format("Azure Webapp '%s' not found.", resourceGroupName));
                    context.setDeploymentState(DeploymentState.UnSuccessful);
                }
            } else {
                context.logError("Error creating resource group:", e);
            }
        }*/
    }

    public interface IValidateWebAppCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public String getAppServiceName();

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();
    }
}
