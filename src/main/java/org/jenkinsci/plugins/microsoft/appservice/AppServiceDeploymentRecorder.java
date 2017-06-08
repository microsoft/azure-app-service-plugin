/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import hudson.FilePath;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
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

import java.io.IOException;

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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        listener.getLogger().println("Starting Azure App Service Deployment");

        // Get app info
        final Azure azureClient = TokenCache.getInstance(credentials.getServicePrincipal()).getAzureClient();
        final WebApp app = azureClient.webApps().getByGroup(appService.getResourceGroupName(), appService.getAppServiceName());
        if (app == null) {
            listener.getLogger().println(String.format("App %s in resource group %s not found",
                appService.getAppServiceName(), appService.getResourceGroupName()));
            return false;
        }

        final String expandedFilePath = build.getEnvironment(listener).expand(filePath);
        final AppServiceDeploymentCommandContext commandContext = new AppServiceDeploymentCommandContext(expandedFilePath);

        commandContext.configure(build, listener, app);

        CommandService.executeCommands(commandContext);

        if (commandContext.getHasError()) {
            return false;
        } else {
            listener.getLogger().println("Done Azure App Service Deployment");
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
