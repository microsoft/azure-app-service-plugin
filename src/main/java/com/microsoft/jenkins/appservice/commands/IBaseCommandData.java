/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

public interface IBaseCommandData {

    Run<?, ?> getRun();

    TaskListener getListener();

    FilePath getWorkspace();

    void logError(String message);

    void logStatus(String status);

    void logError(Exception ex);

    void logError(String prefix, Exception ex);

    void setDeploymentState(DeploymentState deployState);

    DeploymentState getDeploymentState();
}

