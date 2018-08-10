/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.azurecommons.JobContext;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileDeployCommandTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void warDeploy() throws Exception {
        temporaryFolder.newFile("ROOT.war");
        temporaryFolder.newFile("app.war");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        FileDeployCommand.IFileDeployCommandData context = mock(FileDeployCommand.IFileDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.war");

        FileDeployCommand command = new FileDeployCommand();
        command.execute(context);

        verify(app).warDeploy(any(InputStream.class), eq("ROOT"));
        verify(app).warDeploy(any(InputStream.class), eq("app"));
        verify(app, never()).warDeploy(any(InputStream.class), eq("other"));
    }

    @Test
    public void warDeploy_slot() throws Exception {
        temporaryFolder.newFile("ROOT.war");
        temporaryFolder.newFile("app.war");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        FileDeployCommand.IFileDeployCommandData context = mock(FileDeployCommand.IFileDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        DeploymentSlots slots = mock(DeploymentSlots.class);
        when(app.deploymentSlots()).thenReturn(slots);
        DeploymentSlot slot = mock(DeploymentSlot.class);
        when(slots.getByName("slot")).thenReturn(slot);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.war");
        when(context.getSlotName()).thenReturn("slot");

        FileDeployCommand command = new FileDeployCommand();
        command.execute(context);

        verify(slot).warDeploy(any(InputStream.class), eq("ROOT"));
        verify(slot).warDeploy(any(InputStream.class), eq("app"));
        verify(app, never()).warDeploy(any(InputStream.class), anyString());
    }

    @Test
    public void zipDeploy() throws IOException {
        temporaryFolder.newFile("ROOT.zip");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        FileDeployCommand.IFileDeployCommandData context = mock(FileDeployCommand.IFileDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.zip");

        FileDeployCommand command = new FileDeployCommand();
        command.execute(context);

        verify(app).zipDeploy(any(InputStream.class));
    }

    @Test
    public void zipDeploy_slot() throws IOException {
        temporaryFolder.newFile("ROOT.zip");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        FileDeployCommand.IFileDeployCommandData context = mock(FileDeployCommand.IFileDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        DeploymentSlots slots = mock(DeploymentSlots.class);
        when(app.deploymentSlots()).thenReturn(slots);
        DeploymentSlot slot = mock(DeploymentSlot.class);
        when(slots.getByName("slot")).thenReturn(slot);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.zip");
        when(context.getSlotName()).thenReturn("slot");

        FileDeployCommand command = new FileDeployCommand();
        command.execute(context);

        verify(slot).zipDeploy(any(InputStream.class));
    }

    @Test
    public void zipDeploy_duplicateZips() throws IOException {
        temporaryFolder.newFile("ROOT.zip");
        temporaryFolder.newFile("app.zip");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        FileDeployCommand.IFileDeployCommandData context = mock(FileDeployCommand.IFileDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.zip");

        FileDeployCommand command = new FileDeployCommand();
        command.execute(context);

        verify(app, never()).zipDeploy(any(InputStream.class));
    }

}
