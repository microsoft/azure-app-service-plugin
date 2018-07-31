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

public class FileDeployCommand implements ICommand<FileDeployCommand.IWarDeployCommandData> {
    private String aiDeployInfo;
    private String aiDeployFailedInfo;

    @Override
    public void execute(IWarDeployCommandData context) {
        WebApp app = context.getWebApp();
        DeployTypeEnum deployType = context.getDeployType();

        FilePath srcDir = context.getJobContext().getWorkspace().child(Util.fixNull(context.getSourceDirectory()));
        try {
            FilePath[] files = srcDir.list(Util.fixNull(context.getFilePath()));

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
                        throw new IOException("Choose ZIP deployment, but there is no zip files in your "
                                + "setting workspace.");
                    }
                    if (zipFilesPathList.size() > 1) {
                        String errorMsg = String.format("There are %d zip files in the workspace, please check it.",
                                zipFilesPathList.size());
                        throw new IOException(errorMsg);
                    }
                    FilePath file = zipFilesPathList.get(0);
                    context.logStatus("Deploy to app " + file.getBaseName() + " using file: " + file.getRemote());
                    try (InputStream stream = file.read()) {
                        String slotName = context.getSlotName();
                        if (StringUtils.isEmpty(slotName)) {
                            app.zipDeploy(stream);
                        } else {
                            DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
                            if (slot != null) {
                                slot.zipDeploy(stream);
                            } else {
                                throw new IOException("Slot " + slotName + " not found");
                            }
                        }
                    }
                    break;
                case WAR:
                    aiDeployInfo = Constants.AI_WAR_DEPLOY;
                    aiDeployFailedInfo = Constants.AI_WAR_DEPLOY_FAILED;
                    for (FilePath filePath : files) {
                        if (!filePath.getName().toLowerCase().endsWith(Constants.WAR_FILE_EXTENSION)) {
                            context.logStatus(filePath.getName() + " is not WAR file. Will skip.");
                            continue;
                        }

                        context.logStatus("Deploy to app " + filePath.getBaseName() + " using file: "
                                + filePath.getRemote());

                        try (InputStream stream = filePath.read()) {
                            String slotName = context.getSlotName();
                            if (StringUtils.isEmpty(slotName)) {
                                app.warDeploy(stream, filePath.getBaseName());
                            } else {
                                DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
                                if (slot != null) {
                                    slot.warDeploy(stream, filePath.getBaseName());
                                } else {
                                    throw new IOException("Slot " + slotName + " not found");
                                }
                            }
                        }
                    }
                    break;
                default:
                    String errorMsg = String.format("Java app does not support %s deployment now. Please choose WAR"
                            + " or ZIP deployment.", deployType.toString());
                    throw new IOException(errorMsg);
            }

            context.setCommandState(CommandState.Success);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, aiDeployInfo,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()));
        } catch (IOException e) {
            context.logError("Fail to deploy war file due to: " + e.getMessage());
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, aiDeployFailedInfo,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()),
                    "Message", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public interface IWarDeployCommandData extends IBaseCommandData {
        DeployTypeEnum getDeployType();

        String getFilePath();

        String getSourceDirectory();

        WebApp getWebApp();

        String getSlotName();
    }
}
