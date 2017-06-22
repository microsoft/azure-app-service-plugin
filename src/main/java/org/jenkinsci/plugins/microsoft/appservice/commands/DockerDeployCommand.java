/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.management.appservice.WebApp;
import org.apache.commons.collections.map.HashedMap;

public class DockerDeployCommand extends DockerCommand implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    @Override
    public void execute(IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Updating configuration of Azure app service `%s`, with new docker image %s",
                    context.getWebApp().name(), image));

            final WebApp webApp = context.getWebApp();
            final AuthConfig authConfig = dockerBuildInfo.getAuthConfig();
            final WebApp.Update update = webApp.update();
            if (AuthConfig.DEFAULT_SERVER_ADDRESS.equalsIgnoreCase(dockerBuildInfo.getAuthConfig().getRegistryAddress())) {
                update.withPrivateDockerHubImage(image)
                        .withCredentials(authConfig.getUsername(), authConfig.getPassword());
            } else {
                update.withPrivateRegistryImage(image, authConfig.getRegistryAddress())
                        .withCredentials(authConfig.getUsername(), authConfig.getPassword());
            }
            update.withTags(new HashedMap());
            webApp.inner().withKind("app");
            update.apply();
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
    }
}
