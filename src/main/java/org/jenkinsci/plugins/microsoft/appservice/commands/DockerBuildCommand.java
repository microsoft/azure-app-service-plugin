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
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;
import hudson.FilePath;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import java.io.File;

public class DockerBuildCommand extends DockerCommand implements ICommand<DockerBuildCommand.IDockerBuildCommandData> {

    @Override
    public void execute(final IDockerBuildCommandData context) {
        final DockerBuildInfo dockerBuildInfo = context.getDockerBuildInfo();
        context.getListener().getLogger().println(String.format("Begin to build docker image %s:%s",
                dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag()));

        try {
            final String image = getImageFullNameWithTag(dockerBuildInfo);
            final FilePath workspace = context.getBuild().getWorkspace();
            if (workspace == null) {
                throw new AzureCloudException("workspace is not available at this time.");
            }
            final File workspaceDir = new File(workspace.getRemote());
            final File dockerfile = new File(workspaceDir, dockerBuildInfo.getDockerfile());
            if (!dockerfile.exists()) {
                throw new AzureCloudException("Dockerfile cannot be found:" + dockerBuildInfo.getDockerfile());
            }
            context.getListener().getLogger().println("Dockerfile found: " + dockerfile.getAbsolutePath());

            final DockerClient client = getDockerClient(dockerBuildInfo);
            final BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(final BuildResponseItem buildResponseItem) {
                    if (buildResponseItem.isBuildSuccessIndicated()) {
                        context.getListener().getLogger().println("Build successful, the image Id: " + buildResponseItem.getImageId());
                        context.getListener().getLogger().println(buildResponseItem.toString());
                        dockerBuildInfo.setImageid(buildResponseItem.getImageId());
                    } else if (buildResponseItem.isErrorIndicated()) {
                        context.getListener().getLogger().println("Build docker image failed");
                        ResponseItem.ErrorDetail detail = buildResponseItem.getErrorDetail();
                        if (detail != null) {
                            context.getListener().getLogger().println("The error detail: " + detail.toString());
                        }
                        context.setDeploymentState(DeploymentState.HasError);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    context.getListener().getLogger().println("Fail to build docker image:" + throwable.getMessage());
                    context.setDeploymentState(DeploymentState.HasError);
                    super.onError(throwable);
                }
            };
            client.buildImageCmd(dockerfile)
                    .withTags(Sets.newHashSet(image))
                    .exec(callback);
        } catch (AzureCloudException e) {
            context.getListener().getLogger().println("Build failed for " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IDockerBuildCommandData extends IBaseCommandData {
        public DockerBuildInfo getDockerBuildInfo();
    }
}
