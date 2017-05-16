/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import hudson.FilePath;
import org.kohsuke.stapler.DataBoundConstructor;

import org.jenkinsci.plugins.microsoft.services.CommandService;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class AppServiceDeploymentRecorder extends Recorder {

    private final AzureAuth credentials;
    private final AppService appService;
    private final String filePath;

    @DataBoundConstructor
    public AppServiceDeploymentRecorder(
            final AzureAuth credentials,
            final AppService appService,
            final String filePath) {
        this.credentials = credentials;
        this.filePath = filePath;
        this.appService = appService;
    }

    public AzureAuth getCredentials() {
        return credentials;
    }

    public AppService getAppService() {
        return appService;
    }

    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }

    private AppServiceDeploymentCommandContext getCommandContext(FilePath workspacePath) {
        FilePath absFilePath = workspacePath.child(filePath);

        return new AppServiceDeploymentCommandContext(
                credentials.getServicePrincipal(),
                appService.getResourceGroupName(),
                Region.fromName(appService.getAppServicePlan().getRegion()),
                appService.getAppServiceName(),
                appService.getAppServicePlan().getAppServicePlanName(),
                appService.getAppServicePlan().getPricingTier(),
                absFilePath.getRemote(),
                !appService.isCreateNewAppServiceEnabled(),
                !appService.getAppServicePlan().isCreateAppServicePlanEnabled()
        );
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        FilePath workspace = build.getWorkspace();
        FilePath workspacePath = new FilePath(launcher.getChannel(), workspace.getRemote());

        listener.getLogger().println("Starting Azure Container Service Deployment");
        AppServiceDeploymentCommandContext commandContext = getCommandContext(workspacePath);

        commandContext.configure(listener);

        CommandService.executeCommands(commandContext);

        if (commandContext.getHasError()) {
            return false;
        } else {
            listener.getLogger().println("Done Azure Container Service Deployment");
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Publish an Azure WebApp";
        }
    }
}
