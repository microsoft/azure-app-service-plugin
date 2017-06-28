/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.*;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import org.jenkinsci.plugins.microsoft.appservice.AppServiceDeploymentCommandContext;
import org.jenkinsci.plugins.microsoft.appservice.commands.*;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppServiceDeploymentCommandContextTest {

    @Test
    public void getterSetter() throws AzureCloudException {
        AppServiceDeploymentCommandContext ctx = new AppServiceDeploymentCommandContext("sample.war");

        Assert.assertEquals("", ctx.getSourceDirectory());
        Assert.assertEquals("", ctx.getTargetDirectory());
        Assert.assertEquals("sample.war", ctx.getFilePath());
        Assert.assertFalse(ctx.getHasError());
        Assert.assertFalse(ctx.getIsFinished());
        Assert.assertEquals(DeploymentState.Unknown, ctx.getDeploymentState());

        ctx.setSourceDirectory("src");
        Assert.assertEquals("src", ctx.getSourceDirectory());

        ctx.setTargetDirectory("webapps");
        Assert.assertEquals("webapps", ctx.getTargetDirectory());

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
    public void configure() throws AzureCloudException {
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
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(ctx.getStartCommandClass().getName(), GitDeployCommand.class.getName());

        // Java Application
        when(app.javaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        ctx.configure(build, listener, app);
        commands = ctx.getCommands();
        Assert.assertFalse(commands.containsKey(GitDeployCommand.class));
        Assert.assertTrue(commands.containsKey(FTPDeployCommand.class));
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(ctx.getStartCommandClass().getName(), FTPDeployCommand.class.getName());

        // Docker
        ctx.setPublishType(AppServiceDeploymentCommandContext.PUBLISH_TYPE_DOCKER);
        ctx.configure(build, listener, app);
        commands = ctx.getCommands();
        Assert.assertFalse(commands.containsKey(GitDeployCommand.class));
        Assert.assertFalse(commands.containsKey(FTPDeployCommand.class));
        Assert.assertTrue(commands.containsKey(DockerBuildCommand.class));
        Assert.assertTrue(commands.containsKey(DockerPushCommand.class));
        Assert.assertTrue(commands.containsKey(DockerDeployCommand.class));
        Assert.assertEquals(3, commands.size());
        Assert.assertEquals(ctx.getStartCommandClass().getName(), DockerBuildCommand.class.getName());
    }

    @Test
    public void configureSlot() throws AzureCloudException {
        final AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        final BuildListener listener = mock(BuildListener.class);
        final WebApp app = mock(WebApp.class);

        // Mock slot publishing profile
        final PublishingProfile slotPubProfile = mock(PublishingProfile.class);
        when(slotPubProfile.ftpUsername()).thenReturn("slot-user");

        final DeploymentSlot slot = mock(DeploymentSlot.class);
        when(slot.getPublishingProfile()).thenReturn(slotPubProfile);

        final DeploymentSlots slots = mock(DeploymentSlots.class);
        when(slots.getByName("staging")).thenReturn(slot);

        when(app.deploymentSlots()).thenReturn(slots);

        // Mock default publishing profile
        final PublishingProfile defaultPubProfile = mock(PublishingProfile.class);
        when(defaultPubProfile.ftpUsername()).thenReturn("default-user");

        when(app.getPublishingProfile()).thenReturn(defaultPubProfile);

        // Configure default
        AppServiceDeploymentCommandContext ctx = new AppServiceDeploymentCommandContext("sample.war");
        ctx.configure(build, listener, app);
        Assert.assertEquals("default-user", ctx.getPublishingProfile().ftpUsername());

        // Configure slot
        ctx = new AppServiceDeploymentCommandContext("sample.war");
        ctx.setSlotName("staging");
        ctx.configure(build, listener, app);
        Assert.assertEquals("slot-user", ctx.getPublishingProfile().ftpUsername());

        // Configure not existing slot
        try {
            ctx = new AppServiceDeploymentCommandContext("sample.war");
            ctx.setSlotName("not-found");
            ctx.configure(build, listener, app);
            Assert.fail("Should throw exception when slot not found");
        } catch (AzureCloudException ex) {
        }
    }
}
