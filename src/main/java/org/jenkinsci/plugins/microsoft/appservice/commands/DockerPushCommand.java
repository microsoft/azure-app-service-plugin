/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class DockerPushCommand extends DockerCommand implements ICommand<DockerPushCommand.IDockerPushCommandData> {
    @Override
    public void execute(final IDockerPushCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Push docker image `%s` to %s",
                    image, dockerBuildInfo.getAuthConfig().getRegistryAddress()));

            final DockerClient dockerClient = getDockerClient(dockerBuildInfo.getAuthConfig());

            final PushImageResultCallback callback = new PushImageResultCallback() {
                @Override
                public void onNext(final PushResponseItem item) {
                    context.logStatus(outputResponseItem(item));
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    context.logStatus("Fail to push docker image:" + throwable.getMessage());
                    context.setDeploymentState(DeploymentState.HasError);
                    super.onError(throwable);
                }
            };

            try {
                dockerClient.pushImageCmd(image)
                        .withTag(dockerBuildInfo.getDockerImageTag())
                        .exec(callback)
                        .awaitSuccess();
                context.logStatus("Push completed.");
                context.setDeploymentState(DeploymentState.Success);
            } catch (DockerClientException docker) {
                context.logError(docker);
                context.setDeploymentState(DeploymentState.HasError);
            }
        } catch (AzureCloudException e) {
            context.getListener().getLogger().println("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    private String outputResponseItem(final PushResponseItem item) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(item.getId())) {
            stringBuilder.append(item.getId()).append(": ");
        }
        if (StringUtils.isNotBlank(item.getStatus())) {
            stringBuilder.append(item.getStatus());
        }
        if (StringUtils.isNotBlank(item.getProgress())) {
            stringBuilder.append(item.getProgress());
        }
        return stringBuilder.toString();
    }

    public interface IDockerPushCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();
    }
}