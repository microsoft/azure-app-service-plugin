/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.azurecommons.JobContext;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WarDeployCommandTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void execute() throws Exception {
        temporaryFolder.newFile("ROOT.war");
        temporaryFolder.newFile("app.war");
        temporaryFolder.newFile("other.txt");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        WarDeployCommand.IWarDeployCommandData context = mock(WarDeployCommand.IWarDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebApp app = mock(WebApp.class);
        when(context.getWebApp()).thenReturn(app);
        when(context.getFilePath()).thenReturn("*.war");

        WarDeployCommand command = new WarDeployCommand();
        command.execute(context);

        verify(app).warDeploy(any(InputStream.class), eq("ROOT"));
        verify(app).warDeploy(any(InputStream.class), eq("app"));
        verify(app, never()).warDeploy(any(InputStream.class), eq("other"));
    }
}
