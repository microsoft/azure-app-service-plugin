/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;

public class GetPublishSettingsCommand implements ICommand<GetPublishSettingsCommand.IGetPublishSettingsCommandData> {

    @Override
    public void execute(GetPublishSettingsCommand.IGetPublishSettingsCommandData context) {
        try {
            context.logStatus("Retrieving FTP publish settings.");
            String resourceGroupName = context.getResourceGroupName();
            String name = context.getAppServiceName();

            final Azure azureClient = TokenCache.getInstance(context.getAzureServicePrincipal()).getAzureClient();
            final WebApp app = azureClient.webApps().getByGroup(resourceGroupName, name);
            final PublishingProfile pubProfile = app.getPublishingProfile();

            context.setFTPUrl(pubProfile.ftpUrl());
            context.setFTPUserName(pubProfile.ftpUsername());
            context.setFTPPassword(pubProfile.ftpPassword());

            context.setDeploymentState(DeploymentState.Success);
            context.logStatus("Successfully retrieved FTP publish settings");

        } catch (Exception e) {
            context.logError("Error retrieving FTP publish settings: " + e.getMessage());
            e.printStackTrace();
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    public interface IGetPublishSettingsCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public String getAppServiceName();

        public void setFTPUrl(String publishUrl);

        public void setFTPUserName(String userName);

        public void setFTPPassword(String password);

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();
    }
}
