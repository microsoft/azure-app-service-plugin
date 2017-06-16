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

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;

public class DockerDeployCommand extends DockerCommand implements ICommand<DockerDeployCommand.IDockerDeployCommandData> {
    @Override
    public void execute(IDockerDeployCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        context.getListener().getLogger().println(String.format("Begin to deploy to azure web app on linux, the docker image %s:%s",
                dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag()));

        WebApp webApp = context.getWebApp();
//        azureClient.webApps().inner().updateConfiguration()

        WebAppBase.DefinitionStages.WithCreate<WebApp> withCreate;
    }

    public interface IDockerDeployCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();

        public WebApp getWebApp();
    }
}
