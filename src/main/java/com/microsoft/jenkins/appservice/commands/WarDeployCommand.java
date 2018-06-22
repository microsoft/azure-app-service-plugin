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
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import hudson.FilePath;
import hudson.Util;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;

public class WarDeployCommand implements ICommand<WarDeployCommand.IWarDeployCommandData> {

    @Override
    public void execute(IWarDeployCommandData context) {
        WebApp app = context.getWebApp();

        FilePath srcDir = context.getJobContext().getWorkspace().child(Util.fixNull(context.getSourceDirectory()));
        try {
            FilePath[] files = srcDir.list(Util.fixNull(context.getFilePath()));

            for (FilePath file : files) {
                if (!file.getName().toLowerCase().endsWith("war")) {
                    context.logStatus(file.getName() + " is not WAR file. Will skip.");
                    continue;
                }

                context.logStatus("Deploy to app " + file.getBaseName() + " using file: " + file.getRemote());

                try (InputStream stream = file.read()) {
                    String slotName = context.getSlotName();
                    if (StringUtils.isEmpty(slotName)) {
                        app.warDeploy(stream, file.getBaseName());
                    } else {
                        DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
                        if (slot != null) {
                            slot.warDeploy(stream, file.getBaseName());
                        } else {
                            throw new IOException("Slot " + slotName + " not found");
                        }
                    }
                }
            }

            context.setCommandState(CommandState.Success);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_WAR_DEPLOY,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()));
        } catch (IOException e) {
            context.logError("Fail to deploy war file due to: " + e.getMessage());
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_WAR_DEPLOY_FAILED,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebApp().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebApp().name()),
                    "Message", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public interface IWarDeployCommandData extends IBaseCommandData {

        String getFilePath();

        String getSourceDirectory();

        WebApp getWebApp();

        String getSlotName();
    }
}
