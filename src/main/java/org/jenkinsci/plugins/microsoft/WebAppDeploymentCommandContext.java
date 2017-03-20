/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.BuildListener;
import java.util.HashMap;
import org.jenkinsci.plugins.microsoft.commands.AbstractCommandContext;
import org.jenkinsci.plugins.microsoft.commands.CreateWebAppCommand;
import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.commands.GetPublishSettingsCommand;
import org.jenkinsci.plugins.microsoft.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.commands.ICommand;
import org.jenkinsci.plugins.microsoft.commands.ResourceGroupCommand;
import org.jenkinsci.plugins.microsoft.commands.TemplateDeployCommand;
import org.jenkinsci.plugins.microsoft.commands.TemplateMonitorCommand;
import org.jenkinsci.plugins.microsoft.commands.TransitionInfo;
import org.jenkinsci.plugins.microsoft.commands.UploadWarCommand;
import org.jenkinsci.plugins.microsoft.commands.ValidateWebAppCommand;

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
            final String regionName,
            final String webAppName,
            final String appServicePlanName,
            final String filePath) {
        this.servicePrincipal = servicePrincipal;
        this.region = Region.fromName(regionName);
        this.resourceGroupName = resourceGroupName;
        this.webAppName = webAppName;
        this.appServicePlanName = appServicePlanName;
        this.filePath = filePath;
    }

    public void configure(BuildListener listener) {
        HashMap<Class, TransitionInfo> commands = new HashMap<Class, TransitionInfo>();
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
    public String getAppServicePlanName()
    {
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
