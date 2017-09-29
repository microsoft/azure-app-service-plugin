/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.appservice.AzureAppServicePlugin;
import com.microsoft.jenkins.appservice.util.AzureUtils;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class DockerDeployCommand extends DockerCommand
        implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";
    static final String SETTING_REGISTRY_USERNAME = "DOCKER_REGISTRY_SERVER_USERNAME";
    static final String SETTING_REGISTRY_PASSWORD = "DOCKER_REGISTRY_SERVER_PASSWORD";
    static final String SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE = "WEBSITES_ENABLE_APP_SERVICE_STORAGE";

    @Override
    public void execute(final IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        final AuthConfig authConfig = dockerBuildInfo.getAuthConfig();
        final WebApp webApp = (WebApp) context.getWebApp();
        final String slotName = context.getSlotName();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format(
                    "Updating configuration of Azure app service `%s`, with new docker image %s.",
                    context.getWebApp().name(), image));

            if (StringUtils.isNotBlank(context.getSlotName())) {
                context.logStatus(String.format("Targeting deployment slot `%s`.", context.getSlotName()));
            }

            if (StringUtils.isBlank(context.getSlotName())) {
                final WebApp.Update update = webApp.update();
                if (AuthConfig.DEFAULT_SERVER_ADDRESS.equalsIgnoreCase(authConfig.getRegistryAddress())) {
                    update.withPrivateDockerHubImage(image)
                            .withCredentials(authConfig.getUsername(), authConfig.getPassword());
                } else {
                    update.withPrivateRegistryImage(image, authConfig.getRegistryAddress())
                            .withCredentials(authConfig.getUsername(), authConfig.getPassword());
                }

                disableSMBShareIfNotSet(webApp, update);

                update.withTags(new HashedMap());
                webApp.inner().withKind("app");
                update.apply();
                webApp.stop();
                webApp.start();
            } else {
                final DeploymentSlot slot = webApp.deploymentSlots().getByName(slotName);
                checkNotNull(slot, "Deployment slot not found:" + slotName);

                DeploymentSlot.Update update = slot.update();
                update.withAppSetting(SETTING_REGISTRY_SERVER, authConfig.getRegistryAddress());
                update.withAppSetting(SETTING_REGISTRY_USERNAME, authConfig.getUsername());
                update.withAppSetting(SETTING_REGISTRY_PASSWORD, authConfig.getPassword());

                disableSMBShareIfNotSet(slot, update);

                update.apply();

                final AzureCredentials.ServicePrincipal sp = AzureCredentials.getServicePrincipal(
                        context.getAzureCredentialsId());
                final Azure azure = AzureUtils.buildAzureClient(sp);

                final SiteConfigResourceInner siteConfigResourceInner = azure.webApps().inner().getConfigurationSlot(
                        slot.resourceGroupName(), webApp.name(), slotName);
                checkNotNull(siteConfigResourceInner, "Configuration not found for slot:" + slotName);

                siteConfigResourceInner.withLinuxFxVersion(String.format("DOCKER|%s", image));
                azure.webApps().inner().updateConfigurationSlot(
                        webApp.resourceGroupName(), webApp.name(), slot.name(), siteConfigResourceInner);
            }
            context.setCommandState(CommandState.Success);
            context.logStatus("Azure app service updated successfully.");
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_DOCKER_DEPLOY,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()),
                    "Slot", context.getSlotName(),
                    "Image", image);
        } catch (Exception e) {
            context.logError("Fails in updating Azure app service", e);
            context.setCommandState(CommandState.HasError);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_DOCKER_DEPLOY_FAILED,
                    "Message", e.getMessage(),
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()),
                    "Slot", context.getSlotName());
        }
    }

    /**
     * Disable SMB share if not set.
     *
     * This helps improve the reliability for container apps.
     * @see <a href=
     *      "https://docs.microsoft.com/en-us/azure/app-service/containers/app-service-linux-faq#custom-containers">
     *      Azure App Service Web Apps for Containers FAQ</a>
     * @param webApp Web app
     * @param update Update params
     */
    static void disableSMBShareIfNotSet(final WebAppBase webApp, final WebAppBase.Update update) {
        if (!webApp.appSettings().containsKey(SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE)) {
            update.withAppSetting(SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, "false");
        }
    }

    public interface IDockerDeployCommandData extends IBaseCommandData {
        DockerBuildInfo getDockerBuildInfo();

        WebApp getWebApp();

        String getSlotName();

        String getAzureCredentialsId();
    }
}
