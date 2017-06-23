/*
 Copyright 2017 Microsoft Open Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServiceCertificate;
import com.microsoft.azure.management.appservice.NameValuePair;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.util.AzureCredentials;
import org.apache.commons.collections.map.HashedMap;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

import java.util.List;

public class DockerDeployCommand extends DockerCommand implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    @Override
    public void execute(IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        context.logStatus(String.format("Begin to update web app `%s` configuration, the docker image %s:%s",
                context.getWebApp().name(),
                dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag()));

        try {
            final WebApp webApp = context.getWebApp();
            final AuthConfig authConfig = dockerBuildInfo.getAuthConfig();
            final WebApp.Update update = webApp.update();
            if (AuthConfig.DEFAULT_SERVER_ADDRESS.equalsIgnoreCase(dockerBuildInfo.getAuthConfig().getRegistryAddress())) {
                update.withPrivateDockerHubImage(imageAndTag(dockerBuildInfo))
                        .withCredentials(authConfig.getUsername(), authConfig.getPassword());
            } else {
                update.withPrivateRegistryImage(imageAndTag(dockerBuildInfo), authConfig.getRegistryAddress())
                        .withCredentials(authConfig.getUsername(), authConfig.getPassword());
            }
            update.withTags(new HashedMap());
            webApp.inner().withKind("app");
            update.apply();
            context.setDeploymentState(DeploymentState.Success);
        } catch (Exception e) {
            context.logError("fail to update webapp", e);
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IDockerDeployCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();

        public WebApp getWebApp();
    }
}
