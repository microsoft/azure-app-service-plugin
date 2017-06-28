/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;
import hudson.FilePath;
import hudson.Util;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class DockerBuildCommand extends DockerCommand implements ICommand<DockerBuildCommand.IDockerBuildCommandData> {

    @Override
    public void execute(final IDockerBuildCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Building new docker image `%s`", image));

            final FilePath workspace = context.getBuild().getWorkspace();
            if (workspace == null) {
                throw new AzureCloudException("workspace is not available at this time.");
            }
            final File workspaceDir = new File(workspace.getRemote());
            final FileSet fileSet = Util.createFileSet(workspaceDir, dockerBuildInfo.getDockerfile());
            final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
            if (files.length > 1) {
                context.logStatus("multiple Dockerfile found in the specific path.");
                context.setDeploymentState(DeploymentState.HasError);
                return;
            } else if (files.length == 0) {
                context.logStatus("No Dockerfile found in the specific path.");
                context.setDeploymentState(DeploymentState.HasError);
                return;
            }

            final File dockerfile = new File(workspaceDir, files[0]);
            if (!dockerfile.exists()) {
                throw new AzureCloudException("Dockerfile cannot be found:" + dockerBuildInfo.getDockerfile());
            }
            context.logStatus("Dockerfile found: " + dockerfile.getAbsolutePath());

            final DockerClient client = getDockerClient(dockerBuildInfo.getAuthConfig());
            final BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(final BuildResponseItem buildResponseItem) {
                    if (buildResponseItem.isBuildSuccessIndicated()) {
                        context.logStatus(buildResponseItem.getStream());
                        dockerBuildInfo.setImageId(buildResponseItem.getImageId());
                    } else if (buildResponseItem.isErrorIndicated()) {
                        context.logStatus("Build docker image failed");
                        ResponseItem.ErrorDetail detail = buildResponseItem.getErrorDetail();
                        if (detail != null) {
                            context.logStatus("The error detail: " + detail.toString());
                        }
                        context.setDeploymentState(DeploymentState.HasError);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    context.logStatus("Fail to build docker image:" + throwable.getMessage());
                    context.setDeploymentState(DeploymentState.HasError);
                    super.onError(throwable);
                }
            };
            client.buildImageCmd(dockerfile)
                    .withTags(Sets.newHashSet(image))
                    .exec(callback)
                    .awaitCompletion();
            context.setDeploymentState(DeploymentState.Success);
        } catch (AzureCloudException | InterruptedException e) {
            context.logStatus("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IDockerBuildCommandData extends IBaseCommandData {
        DockerBuildInfo getDockerBuildInfo();
    }
}
