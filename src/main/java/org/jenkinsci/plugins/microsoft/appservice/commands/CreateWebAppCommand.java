/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppServicePricingTier;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

public class CreateWebAppCommand implements ICommand<CreateWebAppCommand.ICreateWebAppCommandData> {

    @Override
    public void execute(CreateWebAppCommand.ICreateWebAppCommandData context) {
        try {
            final Azure azureClient = TokenCache.getInstance(context.getAzureServicePrincipal()).getAzureClient();

            final String resourceGroupName = context.getResourceGroupName();
            final String webAppName = context.getAppServiceName();
            final String appServicePlan = context.getAppServicePlanName();
            final Region region = context.getRegion();
            final AppServicePricingTier pricingTier = context.getAppServicePricingTier();

            if (context.useExistingAppService()) {
                // we don't verify the web app just to be faster.
                context.setDeploymentState(DeploymentState.Success);
                context.logStatus(String.format("Using existing Web App '%s'", webAppName));
            } else {
                if (context.useExistingAppServicePlan()) {
                    context.logStatus(String.format("Creating App Service Plan '%s' if not exist", appServicePlan));
                    AppServicePlan asp = azureClient.appServices().appServicePlans().getByGroup(resourceGroupName, appServicePlan);

                    if (asp == null) {
                        context.setDeploymentState(DeploymentState.UnSuccessful);
                        context.logError(String.format("Could not create App Service Plan '%s'", appServicePlan));
                        return;
                    }

                    context.logStatus(String.format("Creating Web App '%s' if not exist", webAppName));
                    azureClient.webApps()
                            .define(webAppName)
                            .withExistingResourceGroup(resourceGroupName)
                            .withExistingAppServicePlan(asp)
                            .withJavaVersion(JavaVersion.JAVA_8_NEWEST)     // TODO: Give user an option
                            .withWebContainer(WebContainer.TOMCAT_8_0_NEWEST)
                            .create();
                } else {
                    context.logStatus(String.format("Create Web App '%s' with App service Plan'%s' if any doesn't exist", webAppName, appServicePlan));
                    azureClient.webApps()
                            .define(webAppName)
                            .withExistingResourceGroup(resourceGroupName)
                            .withNewAppServicePlan(appServicePlan)
                            .withRegion(region)
                            .withPricingTier(pricingTier)
                            .withJavaVersion(JavaVersion.JAVA_8_NEWEST)     // TODO: Give user an option
                            .withWebContainer(WebContainer.TOMCAT_8_0_NEWEST)
                            .create();
                }
                context.setDeploymentState(DeploymentState.Success);
            }
        } catch (Exception e) {
            context.logError("Error creating the Azure Web App:", e);
        }
    }

    public interface ICreateWebAppCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public Region getRegion();

        public String getAppServiceName();

        public String getAppServicePlanName();

        public AppServicePricingTier getAppServicePricingTier();

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();

        public boolean useExistingAppService();

        public boolean useExistingAppServicePlan();

    }
}
