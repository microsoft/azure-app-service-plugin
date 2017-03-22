/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

public class ResourceGroupCommand implements ICommand<ResourceGroupCommand.IResourceGroupCommandData> {

    @Override
    public void execute(ResourceGroupCommand.IResourceGroupCommandData context) {
        try {
            final String resourceGroupName = context.getResourceGroupName();
            final Region region = context.getRegion();
            context.logStatus(String.format("Creating resource group '%s' if it does not exist", resourceGroupName));
            final Azure azureClient = TokenCache.getInstance(context.getAzureServicePrincipal()).getAzureClient();

            azureClient.resourceGroups().define(resourceGroupName).withRegion(region).create();
            context.setDeploymentState(DeploymentState.Success);
        } catch (Exception e) {
            context.logError("Error creating resource group:", e);
        }
    }

    public interface IResourceGroupCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public Region getRegion();

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();
    }
}
