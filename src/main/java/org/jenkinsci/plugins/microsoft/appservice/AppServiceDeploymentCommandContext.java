/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.appservice.*;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.appservice.commands.*;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class AppServiceDeploymentCommandContext extends AbstractCommandContext
        implements FTPDeployCommand.IFTPDeployCommandData,
        GitDeployCommand.IGitDeployCommandData {

    private final String filePath;
    private String sourceDirectory;
    private String targetDirectory;
    private String slotName;

    private PublishingProfile pubProfile;

    public AppServiceDeploymentCommandContext(final String filePath) {
        this.filePath = filePath;
        this.sourceDirectory = "";
        this.targetDirectory = "";
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = Util.fixNull(sourceDirectory);
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
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

        Class deployCommandClass = null;
        if (app.javaVersion() != JavaVersion.OFF) {
            // For Java application, use FTP-based deployment as it's the recommended way
            deployCommandClass = FTPDeployCommand.class;
            commands.put(FTPDeployCommand.class, new TransitionInfo(new FTPDeployCommand(), null, null));
        } else {
            // For non-Java application, use Git-based deployment
            deployCommandClass = GitDeployCommand.class;
            commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));
        }

        super.configure(build, listener, commands, deployCommandClass);
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

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }
}
