/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

import java.io.IOException;

import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.website.WebSiteManagementClient;
import com.microsoft.azure.management.website.models.Site;

public class ValidateWebappCommand implements ICommand<ValidateWebappCommand.IValidateWebappCommandData> {

    public void execute(ValidateWebappCommand.IValidateWebappCommandData context) {
        String resourceGroupName = context.getResourceGroupName();
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
        }
    }

    public interface IValidateWebappCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public String getWebappName();

        public ResourceManagementClient getResourceClient();

        public WebSiteManagementClient getWebsiteClient();
    }
}
