/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.appservice.AppServicePricingTier;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.SkuDescription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.util.HashMap;

import org.jenkinsci.plugins.gitclient.GitCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.*;

public class AppServiceDeploymentCommandContext extends AbstractCommandContext
        implements ResourceGroupCommand.IResourceGroupCommandData,
        UploadWarCommand.IUploadWarCommandData,
        GitDeployCommand.IGitDeployCommandData,
        CreateWebAppCommand.ICreateWebAppCommandData,
        GetPublishSettingsCommand.IGetPublishSettingsCommandData {

    private final AzureCredentials.ServicePrincipal servicePrincipal;
    private final String resourceGroupName;
    private final Region region;
    private final String appServiceName;
    private final String appServicePlanName;
    private final String filePath;
    private final AppServicePricingTier appServicePricingTier;
    private final boolean useExistingAppService;
    private final boolean useExistingAppServicePlan;

    private PublishingProfile pubProfile;

    public AppServiceDeploymentCommandContext(
            final AzureCredentials.ServicePrincipal servicePrincipal,
            final String resourceGroupName,
            final Region region,
            final String appServiceName,
            final String appServicePlanName,
            final String appServicePricingTier,
            final String filePath,
            final boolean useExistingAppService,
            final boolean useExistingAppServicePlan) {
        this.servicePrincipal = servicePrincipal;
        this.region = region;
        this.resourceGroupName = resourceGroupName;
        this.appServiceName = appServiceName;
        this.appServicePlanName = appServicePlanName;
        this.filePath = filePath;
        this.useExistingAppService = useExistingAppService;
        this.useExistingAppServicePlan = useExistingAppServicePlan;

        String[] tierParts = appServicePricingTier.split("_");
        SkuDescription sd = new SkuDescription();
        sd.withTier(tierParts[0]);
        sd.withSize(tierParts[1]);
        this.appServicePricingTier = AppServicePricingTier.fromSkuDescription(sd);
    }

    public void configure(AbstractBuild<?, ?> build, BuildListener listener) {
        HashMap<Class, TransitionInfo> commands = new HashMap<>();
        commands.put(ResourceGroupCommand.class, new TransitionInfo(new ResourceGroupCommand(), CreateWebAppCommand.class, null));
        commands.put(CreateWebAppCommand.class, new TransitionInfo(new CreateWebAppCommand(), GetPublishSettingsCommand.class, null));
        // TODO: Remove FTP-based deployment after Git-base deployment fully tested
        // commands.put(GetPublishSettingsCommand.class, new TransitionInfo(new GetPublishSettingsCommand(), UploadWarCommand.class, null));
        // commands.put(UploadWarCommand.class, new TransitionInfo(new UploadWarCommand(), null, null));
        commands.put(GetPublishSettingsCommand.class, new TransitionInfo(new GetPublishSettingsCommand(), GitDeployCommand.class, null));
        commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));
        super.configure(build, listener, commands, ResourceGroupCommand.class);
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
    public String getAppServiceName() {
        return appServiceName;
    }

    @Override
    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }

    @Override
    public void setPublishingProfile(PublishingProfile profile) {
        pubProfile = profile;
    }

    @Override
    public AppServicePricingTier getAppServicePricingTier() {
        return appServicePricingTier;
    }

    @Override
    public boolean useExistingAppService() {
        return useExistingAppService;
    }

    @Override
    public boolean useExistingAppServicePlan() {
        return useExistingAppServicePlan;
    }
}
