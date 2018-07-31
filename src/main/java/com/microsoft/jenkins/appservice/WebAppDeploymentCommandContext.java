/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.appservice.commands.DefaultDockerClientBuilder;
import com.microsoft.jenkins.appservice.commands.DockerBuildCommand;
import com.microsoft.jenkins.appservice.commands.DockerBuildInfo;
import com.microsoft.jenkins.appservice.commands.DockerClientBuilder;
import com.microsoft.jenkins.appservice.commands.DockerDeployCommand;
import com.microsoft.jenkins.appservice.commands.DockerPushCommand;
import com.microsoft.jenkins.appservice.commands.DockerRemoveImageCommand;
import com.microsoft.jenkins.appservice.commands.FTPDeployCommand;
import com.microsoft.jenkins.appservice.commands.GitDeployCommand;
import com.microsoft.jenkins.appservice.commands.FileDeployCommand;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.appservice.util.DeployTypeEnum;
import com.microsoft.jenkins.appservice.util.WebAppUtils;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public class WebAppDeploymentCommandContext extends BaseCommandContext
        implements FTPDeployCommand.IFTPDeployCommandData,
        GitDeployCommand.IGitDeployCommandData,
        FileDeployCommand.IWarDeployCommandData,
        DockerBuildCommand.IDockerBuildCommandData,
        DockerPushCommand.IDockerPushCommandData,
        DockerRemoveImageCommand.IDockerRemoveImageCommandData,
        DockerDeployCommand.IDockerDeployCommandData {

    public static final String PUBLISH_TYPE_DOCKER = "docker";

    private final String filePath;
    private DeployTypeEnum deployType;
    private String publishType;
    private DockerBuildInfo dockerBuildInfo;
    private String sourceDirectory;
    private String targetDirectory;
    private String slotName;
    private boolean deleteTempImage;
    private boolean skipDockerBuild;
    private String azureCredentialsId;
    private String subscriptionId;

    private PublishingProfile pubProfile;
    private WebApp webApp;

    public WebAppDeploymentCommandContext(final String filePath) {
        this.filePath = filePath;
        this.sourceDirectory = "";
        this.targetDirectory = "";
    }

    public void setSourceDirectory(final String sourceDirectory) {
        this.sourceDirectory = Util.fixNull(sourceDirectory);
    }

    public void setTargetDirectory(final String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    public void setSlotName(final String slotName) {
        this.slotName = slotName;
    }

    public void setDeployType(final DeployTypeEnum deployType) {
        this.deployType = deployType;
    }

    public void setPublishType(final String publishType) {
        this.publishType = publishType;
    }

    public void setDockerBuildInfo(final DockerBuildInfo dockerBuildInfo) {
        this.dockerBuildInfo = dockerBuildInfo;
    }

    public void setDeleteTempImage(final boolean deleteTempImage) {
        this.deleteTempImage = deleteTempImage;
    }

    public void setSkipDockerBuild(final boolean skipDockerBuild) {
        this.skipDockerBuild = skipDockerBuild;
    }

    public void setAzureCredentialsId(final String azureCredentialsId) {
        this.azureCredentialsId = azureCredentialsId;
    }

    public void configure(
            final Run<?, ?> run,
            final FilePath workspace,
            final Launcher launcher,
            final TaskListener listener,
            final WebApp app) throws AzureCloudException {
        this.webApp = app;

        if (StringUtils.isBlank(slotName)) {
            // Deploy to default
            pubProfile = app.getPublishingProfile();
        } else {
            // Deploy to slot
            final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
            if (slot == null) {
                throw new AzureCloudException(String.format("Slot %s not found", slotName));
            }

            pubProfile = slot.getPublishingProfile();
        }

        AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_START_DEPLOY,
                "Run", AppInsightsUtils.hash(run.getUrl()),
                "Subscription", AppInsightsUtils.hash(subscriptionId),
                "ResourceGroup", AppInsightsUtils.hash(app.resourceGroupName()),
                "WebApp", AppInsightsUtils.hash(app.name()),
                "Slot", slotName);

        CommandService.Builder builder = CommandService.builder();

        Class startCommandClass;
        if (StringUtils.isNotBlank(publishType) && publishType.equalsIgnoreCase(PUBLISH_TYPE_DOCKER)) {
            // Docker deployment
            if (!skipDockerBuild) {
                // Build and push docker image first
                builder.withStartCommand(DockerBuildCommand.class);
                builder.withTransition(DockerBuildCommand.class, DockerPushCommand.class);
                builder.withTransition(DockerPushCommand.class, DockerDeployCommand.class);
                if (deleteTempImage) {
                    builder.withTransition(DockerDeployCommand.class, DockerRemoveImageCommand.class);
                }
            } else {
                // Use existing docker image and skip build step
                builder.withStartCommand(DockerDeployCommand.class);
            }
        } else if (WebAppUtils.isJavaApp(app)) {
            // For Java application, use WAR deployment
            builder.withStartCommand(FileDeployCommand.class);
        } else {
            // For non-Java application, use Git-based deployment
            builder.withStartCommand(GitDeployCommand.class);
        }

        JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        super.configure(jobContext, builder.build());
    }

    @Override
    public StepExecution startImpl(StepContext context) throws Exception {
        return null;
    }

    @Override
    public IBaseCommandData getDataForCommand(final ICommand command) {
        return this;
    }

    @Override
    public DeployTypeEnum getDeployType() {
        return deployType;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    @Override
    public String getTargetDirectory() {
        return targetDirectory;
    }

    public String getPublishType() {
        return publishType;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }

    @Override
    public DockerBuildInfo getDockerBuildInfo() {
        return dockerBuildInfo;
    }

    @Override
    public DockerClientBuilder getDockerClientBuilder() {
        return new DefaultDockerClientBuilder();
    }

    @Override
    public WebApp getWebApp() {
        return webApp;
    }

    @Override
    public WebAppBase getWebAppBase() {
        return webApp;
    }

    @Override
    public String getSlotName() {
        return this.slotName;
    }

    @Override
    public String getAzureCredentialsId() {
        return this.azureCredentialsId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
