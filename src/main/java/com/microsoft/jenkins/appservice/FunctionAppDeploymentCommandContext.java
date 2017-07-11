/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.jenkins.appservice.commands.AbstractCommandContext;
import com.microsoft.jenkins.appservice.commands.DeploymentState;
import com.microsoft.jenkins.appservice.commands.GitDeployCommand;
import com.microsoft.jenkins.appservice.commands.IBaseCommandData;
import com.microsoft.jenkins.appservice.commands.ICommand;
import com.microsoft.jenkins.appservice.commands.TransitionInfo;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.HashMap;

public class FunctionAppDeploymentCommandContext extends AbstractCommandContext
        implements GitDeployCommand.IGitDeployCommandData {

    private final String filePath;
    private String sourceDirectory;
    private String targetDirectory;
    private PublishingProfile pubProfile;

    public FunctionAppDeploymentCommandContext(final String filePath) {
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

    public void configure(Run<?, ?> run, FilePath workspace, TaskListener listener, FunctionApp app) throws AzureCloudException {
        pubProfile = app.getPublishingProfile();

        HashMap<Class, TransitionInfo> commands = new HashMap<>();

        Class startCommandClass = GitDeployCommand.class;
        commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));

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

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }

}
