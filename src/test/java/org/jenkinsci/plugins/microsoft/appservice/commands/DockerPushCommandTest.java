/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by juniwang on 27/06/2017.
 */
public class DockerPushCommandTest extends AbstractDockerCommandTest {

    DockerPushCommand command;
    DockerPushCommand.IDockerPushCommandData commandData;
    DockerClient dockerClient;

    @Before
    public void setup() {
        command = spy(new DockerPushCommand());
        commandData = mock(DockerPushCommand.IDockerPushCommandData.class);
        dockerClient = spy(command.getDockerClient(defaultExampleAuthConfig()));
    }

    @Test
    public void dockerPushCmdTest() throws Exception {
        DockerBuildInfo dockerBuildInfo = defaultExampleBuildInfo();
        when(commandData.getDockerBuildInfo()).thenReturn(dockerBuildInfo);
        when(command.getDockerClient(defaultExampleAuthConfig())).thenReturn(dockerClient);

        PushImageCmd pushImageCmd = mock(PushImageCmd.class);
        when(dockerClient.pushImageCmd(command.imageAndTag(dockerBuildInfo))).thenReturn(pushImageCmd);
        when(pushImageCmd.withTag(dockerBuildInfo.getDockerImageTag())).thenReturn(pushImageCmd);
        PushImageResultCallback callback = mock(PushImageResultCallback.class);
        when(pushImageCmd.exec(any(PushImageResultCallback.class))).thenReturn(callback);
        doNothing().when(callback).awaitSuccess();

        command.execute(commandData);

        verify(dockerClient, times(1)).pushImageCmd(command.imageAndTag(dockerBuildInfo));
        verify(pushImageCmd, times(1)).withTag(dockerBuildInfo.getDockerImageTag());
        verify(pushImageCmd, times(1)).exec(any(PushImageResultCallback.class));
        verify(callback, times(1)).awaitSuccess();
        verify(commandData, times(1)).setDeploymentState(DeploymentState.Success);
    }
}
