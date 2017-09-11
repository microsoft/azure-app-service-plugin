/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.appservice.commands.AbstractCommandContext;
import com.microsoft.jenkins.appservice.commands.DeploymentState;
import com.microsoft.jenkins.appservice.commands.FTPDeployCommand;
import com.microsoft.jenkins.appservice.commands.GitDeployCommand;
import com.microsoft.jenkins.appservice.commands.IBaseCommandData;
import com.microsoft.jenkins.appservice.commands.ICommand;
import com.microsoft.jenkins.appservice.commands.TransitionInfo;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class FunctionAppDeploymentCommandContext extends AbstractCommandContext
        implements FTPDeployCommand.IFTPDeployCommandData, GitDeployCommand.IGitDeployCommandData {

    private final String filePath;
    private String sourceDirectory;
    private String targetDirectory;
    private PublishingProfile pubProfile;
    private FunctionApp functionApp;

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

    public void configure(
            final Run<?, ?> run,
            final FilePath workspace,
            final TaskListener listener,
            final FunctionApp app) throws AzureCloudException {
        this.functionApp = app;

        pubProfile = app.getPublishingProfile();

        HashMap<Class, TransitionInfo> commands = new HashMap<>();

        Class startCommandClass;

        boolean isJava = false;
        try {
            isJava = isJavaFunction(workspace, sourceDirectory, filePath);
        } catch (IOException | InterruptedException e) {
            throw new AzureCloudException(e);
        }

        if (isJava) {
            // For Java function, use FTP-based deployment as it's the recommended way
            startCommandClass = FTPDeployCommand.class;
            commands.put(FTPDeployCommand.class, new TransitionInfo(
                    new FTPDeployCommand(), null, null));
        } else {
            // For non-Java function, use Git-based deployment
            startCommandClass = GitDeployCommand.class;
            commands.put(GitDeployCommand.class, new TransitionInfo(
                    new GitDeployCommand(), null, null));
        }

        super.configure(run, workspace, listener, commands, startCommandClass);
        this.setDeploymentState(DeploymentState.Running);
    }

    static boolean isJavaFunction(final FilePath workspace, final String sourceDirectory, final String filePath)
            throws IOException, InterruptedException {
        FilePath sourceDir = workspace.child(Util.fixNull(sourceDirectory));
        FilePath[] files = sourceDir.list(filePath);

        for (final FilePath file : files) {
            String fileName = file.getName();
            if (fileName.equals("function.json")) {
                String scriptPath = getScriptFileFromConfig(file);
                if (scriptPath.toLowerCase().endsWith(".jar")) {
                    return true;
                }
            }
        }

        return false;
    }

    static String getScriptFileFromConfig(final FilePath filePath) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = filePath.read()) {
            JsonNode root = mapper.readTree(stream);
            JsonNode scriptPathNode = root.get("scriptFile");
            if (scriptPathNode == null) {
                return "";
            }

            return scriptPathNode.asText("");
        }
    }

    @Override
    public IBaseCommandData getDataForCommand(final ICommand command) {
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

    @Override
    public WebAppBase getWebAppBase() {
        return functionApp;
    }
}
