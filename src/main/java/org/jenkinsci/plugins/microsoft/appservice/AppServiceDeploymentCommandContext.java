/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.appservice.commands.*;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import java.util.HashMap;

public class AppServiceDeploymentCommandContext extends AbstractCommandContext
        implements FTPDeployCommand.IFTPDeployCommandData,
        GitDeployCommand.IGitDeployCommandData,
        DockerBuildCommand.IDockerBuildCommandData,
        DockerPushCommand.IDockerPushCommandData,
        DockerRemoveImageCommand.IDockerRemoveImageCommandData,
        DockerDeployCommand.IDockerDeployCommandData {

    public static final String PUBLISH_TYPE_DOCKER = "docker";

    private final String filePath;
    private String publishType;
    private DockerBuildInfo dockerBuildInfo;
    private String sourceDirectory;
    private String targetDirectory;
    private String slotName;
    private boolean deleteTempImage;

    private PublishingProfile pubProfile;
    private WebApp webApp;

    public AppServiceDeploymentCommandContext(final String filePath) {
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

    public void setPublishType(final String publishType) {
        this.publishType = publishType;
    }

    public void setDockerBuildInfo(final DockerBuildInfo dockerBuildInfo) {
        this.dockerBuildInfo = dockerBuildInfo;
    }

    public void setDeleteTempImage(final boolean deleteTempImage) {
        this.deleteTempImage = deleteTempImage;
    }

    public void configure(Run<?, ?> run, FilePath workspace, TaskListener listener, WebApp app) throws AzureCloudException {
        if (StringUtils.isBlank(slotName)) {
            // Deploy to default
            pubProfile = app.getPublishingProfile();
        } else {
            // Deploy to slot
            DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
            if (slot == null) {
                throw new AzureCloudException(String.format("Slot %s not found", slotName));
            }

            pubProfile = slot.getPublishingProfile();
        }

        HashMap<Class, TransitionInfo> commands = new HashMap<>();

        Class startCommandClass;
        if (StringUtils.isNotBlank(publishType) && publishType.equalsIgnoreCase(PUBLISH_TYPE_DOCKER)) {
            startCommandClass = DockerBuildCommand.class;
            this.webApp = app;
            commands.put(DockerBuildCommand.class, new TransitionInfo(new DockerBuildCommand(), DockerPushCommand.class, null));
            commands.put(DockerPushCommand.class, new TransitionInfo(new DockerPushCommand(), DockerDeployCommand.class, null));
            if (deleteTempImage) {
                commands.put(DockerDeployCommand.class, new TransitionInfo(new DockerDeployCommand(), DockerRemoveImageCommand.class, null));
                commands.put(DockerRemoveImageCommand.class, new TransitionInfo(new DockerRemoveImageCommand(), null, null));
            } else {
                commands.put(DockerDeployCommand.class, new TransitionInfo(new DockerDeployCommand(), null, null));
            }
        } else if (app.javaVersion() != JavaVersion.OFF) {
            // For Java application, use FTP-based deployment as it's the recommended way
            startCommandClass = FTPDeployCommand.class;
            commands.put(FTPDeployCommand.class, new TransitionInfo(new FTPDeployCommand(), null, null));
        } else {
            // For non-Java application, use Git-based deployment
            startCommandClass = GitDeployCommand.class;
            commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));
        }

        super.configure(run, workspace, listener, commands, startCommandClass);
        this.setDeploymentState(DeploymentState.Running);
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
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
    public WebApp getWebApp() {
        return webApp;
    }
}
