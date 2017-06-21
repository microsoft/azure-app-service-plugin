/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.microsoft.services.ICommandServiceData;

import java.util.HashMap;

public abstract class AbstractCommandContext implements ICommandServiceData {

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;
    private DeploymentState deployState = DeploymentState.Unknown;
    private HashMap<Class, TransitionInfo> commands;
    private Class startCommandClass;

    protected void configure(Run<?, ?> run, FilePath workspace, TaskListener listener,
                             HashMap<Class, TransitionInfo> commands,
                             Class startCommandClass) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
        this.commands = commands;
        this.startCommandClass = startCommandClass;

    }

    public HashMap<Class, TransitionInfo> getCommands() {
        return commands;
    }

    public Class getStartCommandClass() {
        return startCommandClass;
    }

    public abstract IBaseCommandData getDataForCommand(ICommand command);

    public void setDeploymentState(DeploymentState deployState) {
        this.deployState = deployState;
    }

    public DeploymentState getDeploymentState() {
        return this.deployState;
    }

    public boolean getHasError() {
        return this.deployState.equals(DeploymentState.HasError);
    }

    public boolean getIsFinished() {
        return this.deployState.equals(DeploymentState.HasError)
                || this.deployState.equals(DeploymentState.Done);
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

    public void logStatus(String status) {
        listener.getLogger().println(status);
    }

    public void logError(Exception ex) {
        this.logError("Error: ", ex);
    }

    public void logError(String prefix, Exception ex) {
        this.listener.error(prefix + ex.getMessage());
        ex.printStackTrace();
        this.deployState = DeploymentState.HasError;
    }

    public void logError(String message) {
        this.listener.error(message);
        this.deployState = DeploymentState.HasError;
    }
}
