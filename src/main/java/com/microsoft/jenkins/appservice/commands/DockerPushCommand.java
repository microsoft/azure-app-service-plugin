/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.microsoft.jenkins.appservice.AzureAppServicePlugin;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsConstants;
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
        final JobContext jobContext = context.getJobContext();

        try {
            final String image = imageAndTag(dockerBuildInfo);
            context.logStatus(String.format("Push docker image `%s` to %s",
                    image, dockerBuildInfo.getAuthConfig().getRegistryAddress()));

            final FilePath workspace = jobContext.getWorkspace();

            final CommandState state = workspace.act(new DockerPushCommandOnSlave(
                    jobContext.getTaskListener(), context.getDockerClientBuilder(), dockerBuildInfo, image));

            context.logStatus("Push completed");
            context.setCommandState(state);
            AzureAppServicePlugin.sendEvent(AppInsightsConstants.DOCKER, Constants.AI_DOCKER_PUSH,
                    "Registry", dockerBuildInfo.getAuthConfig().getRegistryAddress());
        } catch (AzureCloudException | InterruptedException | IOException e) {
            context.logStatus("Build failed for " + e.getMessage());
            context.setCommandState(CommandState.HasError);
            AzureAppServicePlugin.sendEvent(AppInsightsConstants.DOCKER, Constants.AI_DOCKER_PUSH_FAILED,
                    "Message", e.getMessage());
        }
    }

    private static final class DockerPushCommandOnSlave
            extends MasterToSlaveCallable<CommandState, AzureCloudException> {

        private final DockerClientBuilder dockerClientBuilder;
        private final TaskListener listener;
        private final DockerBuildInfo dockerBuildInfo;
        private final String image;

        private DockerPushCommandOnSlave(
                final TaskListener listener,
                final DockerClientBuilder dockerClientBuilder,
                final DockerBuildInfo dockerBuildInfo,
                final String image) {
            this.listener = listener;
            this.dockerClientBuilder = dockerClientBuilder;
            this.dockerBuildInfo = dockerBuildInfo;
            this.image = image;
        }

        @Override
        public CommandState call() throws AzureCloudException {
            final CommandState[] state = {CommandState.Success};
            final DockerClient dockerClient = dockerClientBuilder.build(dockerBuildInfo.getAuthConfig());
            final PushImageResultCallback callback = new PushImageResultCallback() {
                @Override
                public void onNext(final PushResponseItem item) {
                    listener.getLogger().println(outputResponseItem(item));
                    super.onNext(item);
                }

                @Override
                public void onError(final Throwable throwable) {
                    listener.getLogger().println("Fail to push docker image:" + throwable.getMessage());
                    state[0] = CommandState.HasError;
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
