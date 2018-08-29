/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.appservice.AzureAppServicePlugin;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.appservice.util.DeployTypeEnum;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import hudson.FilePath;
import hudson.Util;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileDeployCommand implements ICommand<FileDeployCommand.IFileDeployCommandData> {
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private String aiDeployInfo;
    private String aiDeployFailedInfo;

    @Override
    public void execute(IFileDeployCommandData context) {
        WebApp app = context.getWebApp();

        final String filePattern = Util.fixNull(context.getFilePath());
        DeployTypeEnum deployType = getDeployTypeFromFilePattern(filePattern);


        FilePath srcDir = context.getJobContext().getWorkspace().child(Util.fixNull(context.getSourceDirectory()));
        try {
            FilePath[] files = srcDir.list(filePattern);

            switch (deployType) {
                case ZIP:
                    aiDeployInfo = Constants.AI_ZIP_DEPLOY;
                    aiDeployFailedInfo = Constants.AI_ZIP_DEPLOY_FAILED;
                    List<FilePath> zipFilesPathList = new ArrayList<>();
                    for (FilePath file : files) {
                        if (!file.getName().toLowerCase().endsWith(Constants.ZIP_FILE_EXTENSION)) {
                            context.logStatus(file.getName() + " is not ZIP file. Will skip.");
                            continue;
                        }
                        zipFilesPathList.add(file);
                    }
                    if (zipFilesPathList.isEmpty()) {
                        context.logStatus("Choose ZIP deployment, but there is no zip files in your "
                                + "setting workspace.");
                        return;
                    }
                    if (zipFilesPathList.size() > 1) {
                        String errorMsg = String.format("There are %d zip files in the workspace, please check it.",
                                zipFilesPathList.size());
                        throw new IOException(errorMsg);
                    }
                    FilePath file = zipFilesPathList.get(0);
                    context.logStatus("Deploy to app " + file.getBaseName() + " using file: " + file.getRemote());

                    int retryCount = 0;
                    while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
                        retryCount++;
                        try (InputStream stream = file.read()) {
                            String slotName = context.getSlotName();
                            try {
                                if (StringUtils.isEmpty(slotName)) {
                                    app.zipDeploy(stream);
                                } else {
                                    DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
                                    slot.zipDeploy(stream);
                                }
                                context.setCommandState(CommandState.Success);
                                AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, aiDeployInfo,
                                        "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                                        "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                                        "WebApp", AppInsightsUtils.hash(context.getWebApp().name()));
                                return;
                            } catch (Exception e) {
                                context.logStatus(String.format("Exception occurred when deploying "
                                                + "the zip package: %s, retrying immediately (%d/%d)", e.getMessage(),
                                        retryCount, DEFAULT_MAX_RETRY_TIMES));
                            }
                        }
                    }
                    throw new IOException(String.format("The zip deploy failed after %d times of retry.", retryCount));
                case WAR:
                    aiDeployInfo = Constants.AI_WAR_DEPLOY;
                    aiDeployFailedInfo = Constants.AI_WAR_DEPLOY_FAILED;
                    boolean hasWarFile = false;
                    for (FilePath filePath : files) {
                        if (!filePath.getName().toLowerCase().endsWith(Constants.WAR_FILE_EXTENSION)) {
                            context.logStatus(filePath.getName() + " is not WAR file. Will skip.");
                            continue;
                        }
                        context.logStatus("Deploy to app " + filePath.getBaseName() + " using file: "
                                + filePath.getRemote());
                        hasWarFile = true;

                        retryCount = 0;
                        boolean deploySuccess = false;
                        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
                            retryCount++;
                            try (InputStream stream = filePath.read()) {
                                String slotName = context.getSlotName();
                                try {
                                    if (StringUtils.isEmpty(slotName)) {
                                        app.warDeploy(stream, filePath.getBaseName());
                                    } else {
                                        DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
                                        slot.warDeploy(stream, filePath.getBaseName());
                                    }
                                    deploySuccess = true;
                                    break;
                                } catch (Exception e) {
                                    context.logStatus(String.format("Exception occurred when deploying "
                                                    + "the zip package: %s, retrying immediately (%d/%d)",
                                            e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
                                }
                            }
                        }
                        if (!deploySuccess) {
                            throw new IOException(String.format("The war deploy failed after %d times of retry.",
                                    retryCount));
                        }
                    }
                    if (!hasWarFile) {
                        context.logStatus("Choose WAR deployment, but there is no war files in your "
                                + "setting workspace.");
                        return;
                    }
                    context.setCommandState(CommandState.Success);
                    AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, aiDeployInfo,
                            "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                            "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                            "WebApp", AppInsightsUtils.hash(context.getWebApp().name()));
                    break;
                default:
                    String errorMsg = String.format("Java app does not support %s deployment now. Please choose WAR"
                            + " or ZIP deployment.", deployType.toString());
                    throw new IOException(errorMsg);
            }
        } catch (IOException e) {
            String errorMsg = String.format("Fail to deploy %s file due to: %s", deployType.toString(), e.getMessage());
            context.logError(errorMsg);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, aiDeployFailedInfo,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()),
                    "Message", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private DeployTypeEnum getDeployTypeFromFilePattern(String filePath) {
        String[] splits = filePath.split("\\.");
        String extension = splits[splits.length - 1];
        switch (extension) {
            case Constants.ZIP_FILE_EXTENSION:
                return DeployTypeEnum.ZIP;
            case Constants.WAR_FILE_EXTENSION:
                return DeployTypeEnum.WAR;
            default:
                return DeployTypeEnum.UNKNOWN;
        }
    }

    public interface IFileDeployCommandData extends IBaseCommandData {
        String getFilePath();

        String getSourceDirectory();

        WebApp getWebApp();

        String getSlotName();
    }
}
