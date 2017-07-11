/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class DockerPushCommand extends DockerCommand implements ICommand<DockerPushCommand.IDockerPushCommandData> {

    @Override
    public void execute(final IDockerPushCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Push docker image `%s` to %s",
                    image, dockerBuildInfo.getAuthConfig().getRegistryAddress()));

            final FilePath workspace = context.getWorkspace();

            final DeploymentState state = workspace.act(new DockerPushCommandOnSlave(
                    context.getListener(), context.getDockerClientBuilder(), dockerBuildInfo, image));

            context.logStatus("Push completed");
            context.setDeploymentState(state);
        } catch (AzureCloudException | InterruptedException | IOException e) {
            context.getListener().getLogger().println("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    private static final class DockerPushCommandOnSlave extends MasterToSlaveCallable<DeploymentState, AzureCloudException> {

        private final DockerClientBuilder dockerClientBuilder;
        private final TaskListener listener;
        private final DockerBuildInfo dockerBuildInfo;
        private final String image;

        private DockerPushCommandOnSlave(TaskListener listener, DockerClientBuilder dockerClientBuilder,
                                         DockerBuildInfo dockerBuildInfo, String image) {
            this.listener = listener;
            this.dockerClientBuilder = dockerClientBuilder;
            this.dockerBuildInfo = dockerBuildInfo;
            this.image = image;
        }

        @Override
        public DeploymentState call() throws AzureCloudException {
            final DeploymentState[] state = {DeploymentState.Success};
            final DockerClient dockerClient = dockerClientBuilder.build(dockerBuildInfo.getAuthConfig());
            final PushImageResultCallback callback = new PushImageResultCallback() {
                @Override
                public void onNext(final PushResponseItem item) {
                    listener.getLogger().println(outputResponseItem(item));
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    listener.getLogger().println("Fail to push docker image:" + throwable.getMessage());
                    state[0] = DeploymentState.HasError;
                    super.onError(throwable);
                }
            };

            dockerClient.pushImageCmd(image)
                    .withTag(dockerBuildInfo.getDockerImageTag())
                    .exec(callback)
                    .awaitSuccess();

            return state[0];
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
    }

    public interface IDockerPushCommandData extends IBaseCommandData {
        DockerClientBuilder getDockerClientBuilder();
        DockerBuildInfo getDockerBuildInfo();
    }
}
