/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.io.Files;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by juniwang on 26/06/2017.
 */
public class DockerBuildCommandTest extends AbstractDockerCommandTest {
    DockerBuildCommand command;
    DockerBuildCommand.IDockerBuildCommandData commandData;
    FilePath workspace;
    TemporaryFolder dockerfileDir;
    List<File> dockerfiles;
    DockerClient dockerClient;


    private void createTestDockerfile(int n) throws Exception {
        dockerfiles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            File subDir = dockerfileDir.newFolder();
            File dockerfile = new File(subDir, "Dockerfile");
            FileUtils.write(dockerfile, "someContent");
            dockerfiles.add(dockerfile);
        }
    }

    @Before
    public void setup() throws Exception {
        commandData = mock(DockerBuildCommand.IDockerBuildCommandData.class);
        command = spy(new DockerBuildCommand());

        // Create workspace
        File workspaceDir = Files.createTempDir();
        workspaceDir.deleteOnExit();
        workspace = new FilePath(workspaceDir);
        dockerfileDir = new TemporaryFolder(workspaceDir);
        dockerfileDir.create();

        // Mock job context
        final Run run = mock(Run.class);
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = new StreamTaskListener(System.out, Charset.defaultCharset());
        final JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        when(commandData.getJobContext()).thenReturn(jobContext);

        // Mock docker client
        dockerClient = mock(DockerClient.class);
        when(commandData.getDockerClientBuilder()).thenReturn(new MockDockerClientBuilder(dockerClient));
    }

    @Test
    public void noDockerfileTest() throws Exception {
        when(commandData.getDockerBuildInfo()).thenReturn(defaultExampleBuildInfo());
        command.execute(commandData);
        verify(commandData, times(1)).setCommandState(CommandState.HasError);
    }

    @Test
    public void multipleDockerfileTest() throws Exception {
        when(commandData.getDockerBuildInfo()).thenReturn(defaultExampleBuildInfo());
        createTestDockerfile(3);
        command.execute(commandData);
        verify(commandData, times(1)).setCommandState(CommandState.HasError);
    }

    @Test
    public void dockerBuildCmdTest() throws Exception {
        when(commandData.getDockerBuildInfo()).thenReturn(defaultExampleBuildInfo());
        createTestDockerfile(1);

        BuildImageCmd buildImageCmd = mock(BuildImageCmd.class);
        when(dockerClient.buildImageCmd(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any(Set.class))).thenReturn(buildImageCmd);
        BuildImageResultCallback callback = mock(BuildImageResultCallback.class);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(callback);
        when(callback.awaitCompletion()).thenReturn(callback);

        command.execute(commandData);

        verify(dockerClient, times(1)).buildImageCmd(any(File.class));
        verify(buildImageCmd, times(1)).withTags(any(Set.class));
        verify(buildImageCmd, times(1)).exec(any(BuildImageResultCallback.class));
        verify(callback, times(1)).awaitCompletion();
    }
}
