/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.NameValuePair;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.util.AzureCredentials;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import com.microsoft.jenkins.appservice.util.TokenCache;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class DockerDeployCommand extends DockerCommand implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    private static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    private static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";
    private static final String SETTING_REGISTRY_USERNAME = "DOCKER_REGISTRY_SERVER_USERNAME";
    private static final String SETTING_REGISTRY_PASSWORD = "DOCKER_REGISTRY_SERVER_PASSWORD";

    @Override
    public void execute(IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        final AuthConfig authConfig = dockerBuildInfo.getAuthConfig();
        final WebApp webApp = context.getWebApp();
        final String slotName = context.getSlotName();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Updating configuration of Azure app service `%s`, with new docker image %s.",
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
                update.withTags(new HashedMap());
                webApp.inner().withKind("app");
                update.apply();
                webApp.stop();
                webApp.start();
            } else {
                final DeploymentSlot slot = webApp.deploymentSlots().getByName(slotName);
                checkNotNull(slot, "Deployment slot not found:" + slotName);

                final AzureCredentials.ServicePrincipal sp = AzureCredentials.getServicePrincipal(context.getAzureCredentialsId());
                final Azure azure = TokenCache.getInstance(sp).getAzureClient();

                final SiteConfigResourceInner siteConfigResourceInner = azure.webApps().inner().getConfigurationSlot(
                        slot.resourceGroupName(), webApp.name(), slotName);
                checkNotNull(siteConfigResourceInner, "Configuration not found for slot:" + slotName);

                List<NameValuePair> appSettings = new ArrayList<>();
                appSettings.add(new NameValuePair().withName(SETTING_DOCKER_IMAGE).withValue(image));
                appSettings.add(new NameValuePair().withName(SETTING_REGISTRY_SERVER).withValue(authConfig.getRegistryAddress()));
                appSettings.add(new NameValuePair().withName(SETTING_REGISTRY_USERNAME).withValue(authConfig.getUsername()));
                appSettings.add(new NameValuePair().withName(SETTING_REGISTRY_PASSWORD).withValue(authConfig.getPassword()));
                siteConfigResourceInner.withLinuxFxVersion(String.format("DOCKER|%s", image));
                siteConfigResourceInner.withAppSettings(appSettings);
                azure.webApps().inner().updateConfigurationSlot(webApp.resourceGroupName(), webApp.name(), slot.name(), siteConfigResourceInner);
            }
            context.setDeploymentState(DeploymentState.Success);
            context.logStatus("Azure app service updated successfully.");
        } catch (Exception e) {
            context.logError("Fails in updating Azure app service", e);
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IDockerDeployCommandData extends IBaseCommandData {
        DockerBuildInfo getDockerBuildInfo();

        WebApp getWebApp();

        String getSlotName();

        String getAzureCredentialsId();
    }
}
