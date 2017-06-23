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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class DockerPushCommand extends DockerCommand implements ICommand<DockerPushCommand.IDockerPushCommandData> {
    @Override
    public void execute(final IDockerPushCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        context.logStatus(String.format("Begin to push docker image %s:%s to registry %s",
                dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag(), dockerBuildInfo.getAuthConfig().getRegistryAddress()));

        try {
            final String image = imageAndTag(dockerBuildInfo);
            final DockerClient dockerClient = getDockerClient(dockerBuildInfo.getAuthConfig());

            final PushImageResultCallback callback = new PushImageResultCallback() {
                @Override
                public void onNext(final PushResponseItem item) {
                    context.logStatus(item.toString());
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    context.logStatus("Fail to push docker image:" + throwable.getMessage());
                    context.setDeploymentState(DeploymentState.HasError);
                    super.onError(throwable);
                }
            };

            dockerClient.pushImageCmd(image)
                    .withTag(dockerBuildInfo.getDockerImageTag())
                    .exec(callback)
                    .awaitSuccess();
            context.logStatus("Push completed");
            context.setDeploymentState(DeploymentState.Success);
        } catch (AzureCloudException e) {
            context.getListener().getLogger().println("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IDockerPushCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();
    }
}