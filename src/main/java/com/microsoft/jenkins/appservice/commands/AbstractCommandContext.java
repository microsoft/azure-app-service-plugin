/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.microsoft.jenkins.services.ICommandServiceData;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.HashMap;

public abstract class AbstractCommandContext implements ICommandServiceData {

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;
    private DeploymentState deploymentState = DeploymentState.Unknown;
    private HashMap<Class, TransitionInfo> commands;
    private Class startCommandClass;

    protected void configure(
            final Run<?, ?> aRun,
            final FilePath aWorkspace,
            final TaskListener aListener,
            final HashMap<Class, TransitionInfo> aCommands,
            final Class aStartCommandClass) {
        this.run = aRun;
        this.workspace = aWorkspace;
        this.listener = aListener;
        this.commands = aCommands;
        this.startCommandClass = aStartCommandClass;

    }

    public HashMap<Class, TransitionInfo> getCommands() {
        return commands;
    }

    public Class getStartCommandClass() {
        return startCommandClass;
    }

    public abstract IBaseCommandData getDataForCommand(ICommand command);

    public void setDeploymentState(final DeploymentState deployState) {
        this.deploymentState = deployState;
    }

    public DeploymentState getDeploymentState() {
        return this.deploymentState;
    }

    public boolean getHasError() {
        return this.deploymentState.equals(DeploymentState.HasError);
    }

    public boolean getIsFinished() {
        return this.deploymentState.equals(DeploymentState.HasError)
                || this.deploymentState.equals(DeploymentState.Done);
    }

    public Run<?, ?> getRun() {
        return this.run;
    }

    public TaskListener getListener() {
        return this.listener;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public void logStatus(final String status) {
        listener.getLogger().println(status);
    }

    public void logError(final Exception ex) {
        this.logError("Error: ", ex);
    }

    public void logError(final String prefix, final Exception ex) {
        this.listener.error(prefix + ex.getMessage());
        ex.printStackTrace();
        this.deploymentState = DeploymentState.HasError;
    }

    public void logError(final String message) {
        this.listener.error(message);
        this.deploymentState = DeploymentState.HasError;
    }
}
