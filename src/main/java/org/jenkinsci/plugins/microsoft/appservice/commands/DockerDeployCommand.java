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

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

public class DockerDeployCommand extends DockerCommand implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    @Override
    public void execute(IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        context.getListener().getLogger().println(String.format("Begin to update web app configuration, the docker image %s:%s",
                dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag()));

        final WebApp webApp = context.getWebApp();
        final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(context.getAzureCredentialsId())).getAzureClient();
        final SiteConfigResourceInner siteConfig = azureClient.webApps().inner().getConfiguration(webApp.resourceGroupName(), webApp.name());
        siteConfig.withLinuxFxVersion(String.format("DOCKER|%s:%s", dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag()));
        azureClient.webApps().inner().updateConfiguration(webApp.resourceGroupName(), webApp.name(), siteConfig);
    }

    public interface IDockerDeployCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();

        public WebApp getWebApp();

        public String getAzureCredentialsId();
    }
}
