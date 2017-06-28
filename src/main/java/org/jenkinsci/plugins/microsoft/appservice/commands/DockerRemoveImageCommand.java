/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

/**
 * Created by juniwang on 28/06/2017.
 */
public class DockerRemoveImageCommand extends DockerCommand implements ICommand<DockerRemoveImageCommand.IDockerRemoveImageCommandData> {

    @Override
    public void execute(IDockerRemoveImageCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        final String imageId = dockerBuildInfo.getImageId();
        context.logStatus(String.format("Removing docker image `%s` from current build agent.", imageId));

        final DockerClient dockerClient = getDockerClient(dockerBuildInfo.getAuthConfig());
        dockerClient.removeImageCmd(imageId)
                .withForce(true)
                .exec();
        context.logStatus("Remove completed.");
        context.setDeploymentState(DeploymentState.Success);
    }

    public interface IDockerRemoveImageCommandData extends IBaseCommandData {
        DockerBuildInfo getDockerBuildInfo();
    }
}
