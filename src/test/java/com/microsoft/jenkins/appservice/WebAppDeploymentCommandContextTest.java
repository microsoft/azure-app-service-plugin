/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.google.common.collect.ImmutableSet;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.appservice.commands.DockerBuildCommand;
import com.microsoft.jenkins.appservice.commands.DockerDeployCommand;
import com.microsoft.jenkins.appservice.commands.DockerPushCommand;
import com.microsoft.jenkins.appservice.commands.FTPDeployCommand;
import com.microsoft.jenkins.appservice.commands.GitDeployCommand;
import com.microsoft.jenkins.appservice.commands.FileDeployCommand;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebAppDeploymentCommandContextTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void getterSetter() throws AzureCloudException {
        WebAppDeploymentCommandContext ctx = new WebAppDeploymentCommandContext("sample.war");
        ctx.setAzureCredentialsId("sp");
        ctx.setSubscriptionId("00000000-0000-0000-0000-000000000000");

        Assert.assertEquals("", ctx.getSourceDirectory());
        Assert.assertEquals("", ctx.getTargetDirectory());
        Assert.assertEquals("sample.war", ctx.getFilePath());
        Assert.assertFalse(ctx.getLastCommandState().isError());
        Assert.assertFalse(ctx.getLastCommandState().isFinished());
        Assert.assertEquals(CommandState.Unknown, ctx.getLastCommandState());

        ctx.setSourceDirectory("src");
        Assert.assertEquals("src", ctx.getSourceDirectory());

        ctx.setTargetDirectory("webapps");
        Assert.assertEquals("webapps", ctx.getTargetDirectory());

        final PublishingProfile pubProfile = mock(PublishingProfile.class);
        when(pubProfile.ftpUrl()).thenReturn("ftp://example.com");
        when(pubProfile.ftpUsername()).thenReturn("user");
        when(pubProfile.ftpPassword()).thenReturn("pass");

        final Run run = mock(Run.class);
        final TaskListener listener = mock(TaskListener.class);
        final FilePath workspace = new FilePath(new File("workspace"));
        final Launcher launcher = mock(Launcher.class);
        final WebApp app = mock(WebApp.class);
        when(app.getPublishingProfile()).thenReturn(pubProfile);

        ctx.configure(run, workspace, launcher, listener, app);

        Assert.assertEquals(workspace, ctx.getJobContext().getWorkspace());
        Assert.assertEquals("ftp://example.com", ctx.getPublishingProfile().ftpUrl());
        Assert.assertEquals("user", ctx.getPublishingProfile().ftpUsername());
        Assert.assertEquals("pass", ctx.getPublishingProfile().ftpPassword());
    }

    @Test
    public void configure() throws AzureCloudException {
        WebAppDeploymentCommandContext ctx = new WebAppDeploymentCommandContext("sample.war");
        ctx.setAzureCredentialsId("sp");
        ctx.setSubscriptionId("00000000-0000-0000-0000-000000000000");

        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(new File("workspace"));
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = mock(TaskListener.class);
        final WebApp app = mock(WebApp.class);

        // Non-Java Application
        when(app.javaVersion()).thenReturn(JavaVersion.OFF);
        ctx.configure(run, workspace, launcher, listener, app);
        CommandService commandService = ctx.getCommandService();
        ImmutableSet<Class> commands = commandService.getRegisteredCommands();
        Assert.assertTrue(commands.contains(GitDeployCommand.class));
        Assert.assertFalse(commands.contains(FTPDeployCommand.class));
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(commandService.getStartCommandClass(), GitDeployCommand.class);

        // Java Application
        when(app.javaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        ctx.configure(run, workspace, launcher, listener, app);
        commandService = ctx.getCommandService();
        commands = commandService.getRegisteredCommands();
        Assert.assertFalse(commands.contains(GitDeployCommand.class));
        Assert.assertFalse(commands.contains(FTPDeployCommand.class));
        Assert.assertTrue(commands.contains(FileDeployCommand.class));
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(commandService.getStartCommandClass(), FileDeployCommand.class);

        // Docker
        ctx.setPublishType(WebAppDeploymentCommandContext.PUBLISH_TYPE_DOCKER);
        ctx.configure(run, workspace, launcher, listener, app);
        commandService = ctx.getCommandService();
        commands = commandService.getRegisteredCommands();
        Assert.assertFalse(commands.contains(GitDeployCommand.class));
        Assert.assertFalse(commands.contains(FTPDeployCommand.class));
        Assert.assertTrue(commands.contains(DockerBuildCommand.class));
        Assert.assertTrue(commands.contains(DockerPushCommand.class));
        Assert.assertTrue(commands.contains(DockerDeployCommand.class));
        Assert.assertEquals(3, commands.size());
        Assert.assertEquals(commandService.getStartCommandClass(), DockerBuildCommand.class);

        // Docker without build step
        ctx.setSkipDockerBuild(true);
        ctx.setPublishType(WebAppDeploymentCommandContext.PUBLISH_TYPE_DOCKER);
        ctx.configure(run, workspace, launcher, listener, app);
        commandService = ctx.getCommandService();
        commands = commandService.getRegisteredCommands();
        Assert.assertFalse(commands.contains(GitDeployCommand.class));
        Assert.assertFalse(commands.contains(FTPDeployCommand.class));
        Assert.assertFalse(commands.contains(DockerBuildCommand.class));
        Assert.assertFalse(commands.contains(DockerPushCommand.class));
        Assert.assertTrue(commands.contains(DockerDeployCommand.class));
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals(commandService.getStartCommandClass(), DockerDeployCommand.class);
    }

    @Test
    public void configureSlot() throws AzureCloudException {
        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(new File("workspace"));
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = mock(TaskListener.class);
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
        WebAppDeploymentCommandContext ctx = new WebAppDeploymentCommandContext("sample.war");
        ctx.setAzureCredentialsId("sp");
        ctx.setSubscriptionId("00000000-0000-0000-0000-000000000000");
        ctx.configure(run, workspace, launcher, listener, app);
        Assert.assertEquals("default-user", ctx.getPublishingProfile().ftpUsername());

        // Configure slot
        ctx = new WebAppDeploymentCommandContext("sample.war");
        ctx.setAzureCredentialsId("sp");
        ctx.setSubscriptionId("00000000-0000-0000-0000-000000000000");
        ctx.setSlotName("staging");
        ctx.configure(run, workspace, launcher, listener, app);
        Assert.assertEquals("slot-user", ctx.getPublishingProfile().ftpUsername());

        // Configure not existing slot
        try {
            ctx = new WebAppDeploymentCommandContext("sample.war");
            ctx.setAzureCredentialsId("sp");
            ctx.setSubscriptionId("00000000-0000-0000-0000-000000000000");
            ctx.setSlotName("not-found");
            ctx.configure(run, workspace, launcher, listener, app);
            Assert.fail("Should throw exception when slot not found");
        } catch (AzureCloudException ex) {
        }
    }
}
