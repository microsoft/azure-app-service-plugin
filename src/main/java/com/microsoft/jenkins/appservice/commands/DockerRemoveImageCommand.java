/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import jenkins.security.MasterToSlaveCallable;

import java.io.IOException;

/**
 * Created by juniwang on 28/06/2017.
 */
public class DockerRemoveImageCommand extends DockerCommand
        implements ICommand<DockerRemoveImageCommand.IDockerRemoveImageCommandData> {

    @Override
    public void execute(final IDockerRemoveImageCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        final String imageId = dockerBuildInfo.getImageId();
        context.logStatus(String.format("Removing docker image `%s` from current build agent.", imageId));

        try {
            context.getWorkspace().act(new DockerRemoveCommandOnSlave(
                    context.getDockerClientBuilder(), dockerBuildInfo, imageId));
            context.logStatus("Remove completed.");
            context.setDeploymentState(DeploymentState.Success);
        } catch (IOException | InterruptedException | AzureCloudException e) {
            context.logError("Fail to remove docker image: " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    private static final class DockerRemoveCommandOnSlave extends MasterToSlaveCallable<Void, AzureCloudException> {
        private final DockerClientBuilder dockerClientBuilder;
        private final DockerBuildInfo dockerBuildInfo;
        private final String imageId;

        private DockerRemoveCommandOnSlave(
                final DockerClientBuilder dockerClientBuilder,
                final DockerBuildInfo dockerBuildInfo,
                final String imageId) {
            this.dockerClientBuilder = dockerClientBuilder;
            this.dockerBuildInfo = dockerBuildInfo;
            this.imageId = imageId;
        }

        @Override
        public Void call() throws AzureCloudException {
            final DockerClient dockerClient = dockerClientBuilder.build(dockerBuildInfo.getAuthConfig());
            dockerClient.removeImageCmd(imageId)
                    .withForce(true)
                    .exec();
            return null;
        }
    }

    public interface IDockerRemoveImageCommandData extends IBaseCommandData {
        DockerClientBuilder getDockerClientBuilder();
        DockerBuildInfo getDockerBuildInfo();
    }
}
