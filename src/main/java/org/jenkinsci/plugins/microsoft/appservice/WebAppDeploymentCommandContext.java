/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.BuildListener;
import java.util.HashMap;
import org.jenkinsci.plugins.microsoft.appservice.commands.AbstractCommandContext;
import org.jenkinsci.plugins.microsoft.appservice.commands.CreateWebAppCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.appservice.commands.GetPublishSettingsCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.appservice.commands.ICommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.ResourceGroupCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TemplateDeployCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TemplateMonitorCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TransitionInfo;
import org.jenkinsci.plugins.microsoft.appservice.commands.UploadWarCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.ValidateWebAppCommand;

public class WebAppDeploymentCommandContext extends AbstractCommandContext
        implements ResourceGroupCommand.IResourceGroupCommandData,
        UploadWarCommand.IUploadWarCommandData,
        ValidateWebAppCommand.IValidateWebAppCommandData,
        CreateWebAppCommand.ICreateWebAppCommandData,
        GetPublishSettingsCommand.IGetPublishSettingsCommandData,
        TemplateDeployCommand.ITemplateDeployCommandData,
        TemplateMonitorCommand.ITemplateMonitorCommandData {

    private final AzureCredentials.ServicePrincipal servicePrincipal;
    private final String resourceGroupName;
    private final Region region;
    private final String webAppName;
    private final String appServicePlanName;
    private final String filePath;

    public WebAppDeploymentCommandContext(
            final AzureCredentials.ServicePrincipal servicePrincipal,
            final String resourceGroupName,
            final Region region,
            final String webAppName,
            final String appServicePlanName,
            final String filePath) {
        this.servicePrincipal = servicePrincipal;
        this.region = region;
        this.resourceGroupName = resourceGroupName;
        this.webAppName = webAppName;
        this.appServicePlanName = appServicePlanName;
        this.filePath = filePath;
    }

    public void configure(BuildListener listener) {
        HashMap<Class, TransitionInfo> commands = new HashMap<>();
        commands.put(ResourceGroupCommand.class, new TransitionInfo(new ResourceGroupCommand(), CreateWebAppCommand.class, null));
        commands.put(CreateWebAppCommand.class, new TransitionInfo(new CreateWebAppCommand(), ValidateWebAppCommand.class, null));
        commands.put(ValidateWebAppCommand.class, new TransitionInfo(new ValidateWebAppCommand(), GetPublishSettingsCommand.class, TemplateDeployCommand.class));
        commands.put(GetPublishSettingsCommand.class, new TransitionInfo(new GetPublishSettingsCommand(), UploadWarCommand.class, null));
        commands.put(TemplateDeployCommand.class, new TransitionInfo(new TemplateDeployCommand(), TemplateMonitorCommand.class, null));
        commands.put(TemplateMonitorCommand.class, new TransitionInfo(new TemplateMonitorCommand(), GetPublishSettingsCommand.class, null));
        commands.put(UploadWarCommand.class, new TransitionInfo(new UploadWarCommand(), null, null));
        super.configure(listener, commands, ResourceGroupCommand.class);
        this.setDeploymentState(DeploymentState.Running);
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }

    @Override
    public AzureCredentials.ServicePrincipal getAzureServicePrincipal() {
        return servicePrincipal;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public String getResourceGroupName() {
        return resourceGroupName;
    }

    @Override
    public String getWebappName() {
        return webAppName;
    }

    @Override
    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    public String getPublishUrl() {
        return "";
    }

    @Override
    public String getUserName() {
        return "";
    }

    @Override
    public String getPassWord() {
        return "";
    }
}
