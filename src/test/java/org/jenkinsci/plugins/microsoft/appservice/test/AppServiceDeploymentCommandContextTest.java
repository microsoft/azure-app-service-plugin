/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import org.jenkinsci.plugins.microsoft.appservice.AppServiceDeploymentCommandContext;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.appservice.commands.GitDeployCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TransitionInfo;
import org.jenkinsci.plugins.microsoft.appservice.commands.FTPDeployCommand;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppServiceDeploymentCommandContextTest {

    @Test
    public void getterSetter() {
        AppServiceDeploymentCommandContext ctx = new AppServiceDeploymentCommandContext("sample.war");

        Assert.assertEquals("sample.war", ctx.getFilePath());
        Assert.assertFalse(ctx.getHasError());
        Assert.assertFalse(ctx.getIsFinished());
        Assert.assertEquals(DeploymentState.Unknown, ctx.getDeploymentState());

        final PublishingProfile pubProfile = mock(PublishingProfile.class);
        when(pubProfile.ftpUrl()).thenReturn("ftp://example.com");
        when(pubProfile.ftpUsername()).thenReturn("user");
        when(pubProfile.ftpPassword()).thenReturn("pass");

        final AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        final BuildListener listener = mock(BuildListener.class);
        final WebApp app = mock(WebApp.class);
        when(app.getPublishingProfile()).thenReturn(pubProfile);

        ctx.configure(build, listener, app);

        Assert.assertEquals("ftp://example.com", ctx.getPublishingProfile().ftpUrl());
        Assert.assertEquals("user", ctx.getPublishingProfile().ftpUsername());
        Assert.assertEquals("pass", ctx.getPublishingProfile().ftpPassword());
        Assert.assertEquals(DeploymentState.Running, ctx.getDeploymentState());
    }

    @Test
    public void configure() {
        AppServiceDeploymentCommandContext ctx = new AppServiceDeploymentCommandContext("sample.war");

        final AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        final BuildListener listener = mock(BuildListener.class);
        final WebApp app = mock(WebApp.class);

        // Non-Java Application
        when(app.javaVersion()).thenReturn(JavaVersion.OFF);
        ctx.configure(build, listener, app);
        HashMap<Class, TransitionInfo> commands = ctx.getCommands();
        Assert.assertTrue(commands.containsKey(GitDeployCommand.class));
        Assert.assertFalse(commands.containsKey(FTPDeployCommand.class));

        // Java Application
        when(app.javaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        ctx.configure(build, listener, app);
        commands = ctx.getCommands();
        Assert.assertFalse(commands.containsKey(GitDeployCommand.class));
        Assert.assertTrue(commands.containsKey(FTPDeployCommand.class));
    }
}
