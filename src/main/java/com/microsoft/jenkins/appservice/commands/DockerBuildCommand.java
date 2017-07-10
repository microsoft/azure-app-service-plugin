/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import com.microsoft.jenkins.exceptions.AzureCloudException;

import java.io.File;
import java.io.IOException;

public class DockerBuildCommand extends DockerCommand implements ICommand<DockerBuildCommand.IDockerBuildCommandData> {

    @Override
    public void execute(final IDockerBuildCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Building new docker image `%s`", image));

            final FilePath workspace = context.getWorkspace();
            if (workspace == null) {
                throw new AzureCloudException("workspace is not available at this time.");
            }

            final FilePath dockerfile = findDockerFile(workspace, dockerBuildInfo.getDockerfile());
            context.logStatus("Dockerfile found: " + dockerfile.getRemote());

            final String imageId = workspace.act(new DockerBuildCommandOnSlave(
                    context.getListener(), context.getDockerClientBuilder(), dockerBuildInfo, dockerfile, image));
            dockerBuildInfo.setImageId(imageId);

            context.setDeploymentState(DeploymentState.Success);
        } catch (AzureCloudException | InterruptedException | IOException e) {
            context.logStatus("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    private FilePath findDockerFile(FilePath workspace, String pattern) throws AzureCloudException {
        try {
            final FilePath[] files = workspace.list(pattern);
            if (files.length > 1) {
                throw new AzureCloudException("Multiple Dockerfile found in the specified path.");
            } else if (files.length == 0) {
                throw new AzureCloudException("No Dockerfile found in the specific path.");
            }

            final FilePath dockerfile = files[0];
            if (!dockerfile.exists()) {
                throw new AzureCloudException("Dockerfile cannot be found:" + pattern);
            }

            return dockerfile;
        } catch (IOException | InterruptedException e) {
            throw new AzureCloudException(e);
        }
    }

    private static final class DockerBuildCommandOnSlave extends MasterToSlaveCallable<String, AzureCloudException> {

        private final TaskListener listener;
        private final DockerClientBuilder dockerClientBuilder;
        private final DockerBuildInfo dockerBuildInfo;
        private final FilePath dockerfile;
        private final String image;

        private DockerBuildCommandOnSlave(TaskListener listener, DockerClientBuilder dockerClientBuilder,
                                          DockerBuildInfo buildInfo, FilePath dockerfile, String image) {
            this.listener = listener;
            this.dockerClientBuilder = dockerClientBuilder;
            this.dockerBuildInfo = buildInfo;
            this.dockerfile = dockerfile;
            this.image = image;
        }

        @Override
        public String call() throws AzureCloudException {
            final boolean[] hasError = {false};
            final DockerClient client = dockerClientBuilder.build(dockerBuildInfo.getAuthConfig());
            final BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(final BuildResponseItem buildResponseItem) {
                    if (buildResponseItem.isBuildSuccessIndicated()) {
                        listener.getLogger().println(buildResponseItem.getStream());
                        dockerBuildInfo.setImageId(buildResponseItem.getImageId());
                    } else if (buildResponseItem.isErrorIndicated()) {
                        listener.getLogger().println("Build docker image failed");
                        ResponseItem.ErrorDetail detail = buildResponseItem.getErrorDetail();
                        if (detail != null) {
                            listener.getLogger().println("The error detail: " + detail.toString());
                        }
                        hasError[0] = true;
                    } else if (StringUtils.isNotBlank(buildResponseItem.getStream())) {
                        listener.getLogger().println(buildResponseItem.getStream());
                    }
                    super.onNext(buildResponseItem);
                }

                @Override
                public void onError(Throwable throwable) {
                    listener.getLogger().println("Fail to build docker image:" + throwable.getMessage());
                    hasError[0] = true;
                    super.onError(throwable);
                }
            };

            try {
                client.buildImageCmd(new File(dockerfile.getRemote()))
                        .withTags(Sets.newHashSet(image))
                        .exec(callback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                throw new AzureCloudException(e);
            }

            if (hasError[0]) {
                throw new AzureCloudException("Fail to build docker image");
            }

            return dockerBuildInfo.getImageId();
        }
    }

    public interface IDockerBuildCommandData extends IBaseCommandData {
        DockerBuildInfo getDockerBuildInfo();

        DockerClientBuilder getDockerClientBuilder();
    }
}
