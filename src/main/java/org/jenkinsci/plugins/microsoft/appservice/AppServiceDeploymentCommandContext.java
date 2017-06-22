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
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.appservice.commands.*;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import java.util.HashMap;

public class AppServiceDeploymentCommandContext extends AbstractCommandContext
        implements FTPDeployCommand.IFTPDeployCommandData,
        GitDeployCommand.IGitDeployCommandData,
        DockerBuildCommand.IDockerBuildCommandData,
        DockerPushCommand.IDockerPushCommandData,
        DockerDeployCommand.IDockerDeployCommandData {

    public static final String PUBLISH_TYPE_DOCKER = "docker";

    private final String filePath;
    private final String publishType;
    private final String slotName;
    private final DockerBuildInfo dockerBuildInfo;

    private PublishingProfile pubProfile;
    private WebApp webApp;

    public AppServiceDeploymentCommandContext(final String filePath,
                                              final String publishType,
                                              final String slotName,
                                              final DockerBuildInfo dockerBuildInfo) {
        this.filePath = filePath;
        this.slotName = slotName;
        this.publishType = publishType;
        this.dockerBuildInfo = dockerBuildInfo;
    }

    public void configure(AbstractBuild<?, ?> build, BuildListener listener, WebApp app) throws AzureCloudException {
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
        if (publishType.equalsIgnoreCase(PUBLISH_TYPE_DOCKER)) {
            startCommandClass = DockerBuildCommand.class;
            this.webApp = app;
            commands.put(DockerBuildCommand.class, new TransitionInfo(new DockerBuildCommand(), DockerPushCommand.class, null));
            commands.put(DockerPushCommand.class, new TransitionInfo(new DockerPushCommand(), DockerDeployCommand.class, null));
            commands.put(DockerDeployCommand.class, new TransitionInfo(new DockerDeployCommand(), null, null));
        } else if (app.javaVersion() != JavaVersion.OFF) {
            // For Java application, use FTP-based deployment as it's the recommended way
            startCommandClass = FTPDeployCommand.class;
            commands.put(FTPDeployCommand.class, new TransitionInfo(new FTPDeployCommand(), null, null));
        } else {
            // For non-Java application, use Git-based deployment
            startCommandClass = GitDeployCommand.class;
            commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));
        }

        super.configure(build, listener, commands, startCommandClass);
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
