/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.util.TokenCache;

public class CreateWebAppCommand implements ICommand<CreateWebAppCommand.ICreateWebAppCommandData> {

    @Override
    public void execute(CreateWebAppCommand.ICreateWebAppCommandData context) {
        try {
            final Azure azureClient = TokenCache.getInstance(context.getAzureServicePrincipal()).getAzureClient();

            final String resourceGroupName = context.getResourceGroupName();
            final String webAppName = context.getWebappName();
            final String appServicePlan = context.getAppServicePlanName();

            AppServicePlan asp = azureClient.appServices().appServicePlans().getByGroup(resourceGroupName, appServicePlan);
            if (asp == null) {
                context.logError("The provided App Service Plan doesn't exist");
                context.setDeploymentState(DeploymentState.UnSuccessful);
            } else {
                azureClient.webApps().define(webAppName).withExistingResourceGroup(resourceGroupName).withExistingAppServicePlan(asp).create();
                context.setDeploymentState(DeploymentState.Success);
            }
        } catch (Exception e) {
            context.logError("Error creating the Azure Web App:", e);
        }
    }

    public interface ICreateWebAppCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public String getWebappName();

        public String getAppServicePlanName();

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();

    }
}
