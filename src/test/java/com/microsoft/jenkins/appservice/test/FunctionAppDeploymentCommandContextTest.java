/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.test;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.jenkins.appservice.FunctionAppDeploymentCommandContext;
import com.microsoft.jenkins.appservice.commands.DeploymentState;
import com.microsoft.jenkins.appservice.commands.GitDeployCommand;
import com.microsoft.jenkins.appservice.commands.TransitionInfo;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FunctionAppDeploymentCommandContextTest {

    @Test
    public void getterSetter() throws AzureCloudException {
        FunctionAppDeploymentCommandContext ctx = new FunctionAppDeploymentCommandContext("**/*.js,**/*.json");

        Assert.assertEquals("", ctx.getSourceDirectory());
        Assert.assertEquals("", ctx.getTargetDirectory());
        Assert.assertEquals("**/*.js,**/*.json", ctx.getFilePath());
        Assert.assertFalse(ctx.getHasError());
        Assert.assertFalse(ctx.getIsFinished());
        Assert.assertEquals(DeploymentState.Unknown, ctx.getDeploymentState());

        ctx.setSourceDirectory("src");
        Assert.assertEquals("src", ctx.getSourceDirectory());

        ctx.setTargetDirectory("target");
        Assert.assertEquals("target", ctx.getTargetDirectory());

        final PublishingProfile pubProfile = mock(PublishingProfile.class);
        when(pubProfile.ftpUrl()).thenReturn("ftp://example.com");
        when(pubProfile.ftpUsername()).thenReturn("user");
        when(pubProfile.ftpPassword()).thenReturn("pass");

        final Run run = mock(Run.class);
        final TaskListener listener = mock(TaskListener.class);
        final FilePath workspace = new FilePath(new File("workspace"));
        final FunctionApp app = mock(FunctionApp.class);
        when(app.getPublishingProfile()).thenReturn(pubProfile);

        ctx.configure(run, workspace, listener, app);

        Assert.assertEquals(workspace, ctx.getWorkspace());
        Assert.assertEquals("ftp://example.com", ctx.getPublishingProfile().ftpUrl());
        Assert.assertEquals("user", ctx.getPublishingProfile().ftpUsername());
        Assert.assertEquals("pass", ctx.getPublishingProfile().ftpPassword());
        Assert.assertEquals(DeploymentState.Running, ctx.getDeploymentState());
    }

    @Test
    public void configure() throws AzureCloudException {
        FunctionAppDeploymentCommandContext ctx = new FunctionAppDeploymentCommandContext("**/*.js,**/*.json");

        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(new File("workspace"));
        final TaskListener listener = mock(TaskListener.class);
        final FunctionApp app = mock(FunctionApp.class);

        ctx.configure(run, workspace, listener, app);
        HashMap<Class, TransitionInfo> commands = ctx.getCommands();
        Assert.assertTrue(commands.containsKey(GitDeployCommand.class));
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(ctx.getStartCommandClass().getName(), GitDeployCommand.class.getName());
    }
}
