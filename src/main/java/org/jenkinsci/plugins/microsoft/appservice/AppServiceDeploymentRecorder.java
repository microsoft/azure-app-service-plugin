/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.ctc.wstx.util.StringUtil;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Util;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.kohsuke.stapler.DataBoundConstructor;

import org.jenkinsci.plugins.microsoft.services.CommandService;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;
import org.jenkinsci.plugins.microsoft.services.CommandService;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;

public class AppServiceDeploymentRecorder extends Recorder {

    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String appService;
    private String publishType;
    private String filePath;
    private String dockerRegistry;
    private String dockerFilePath;
    private String dockerRegistryUserName;
    private String dockerRegistryPassword;
    private
    @CheckForNull
    String slotName;

    @DataBoundConstructor
    public AppServiceDeploymentRecorder(
            final String azureCredentialsId,
            final String appService,
            final String resourceGroup) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.appService = appService;
    }

    @DataBoundSetter
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @DataBoundSetter
    public void setPublishType(final String publishType) {
        this.publishType = publishType;
    }

    @DataBoundSetter
    public void setDockerRegistry(final String dockerRegistry) {
        this.dockerRegistry = dockerRegistry;
    }

    @DataBoundSetter
    public void setDockerFilePath(final String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }

    @DataBoundSetter
    public void setDockerRegistryUserName(final String dockerRegistryUserName) {
        this.dockerRegistryUserName = dockerRegistryUserName;
    }

    @DataBoundSetter
    public void setDockerRegistryPassword(final String dockerRegistryPassword) {
        this.dockerRegistryPassword = dockerRegistryPassword;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    public String getAppService() {
        return appService;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getPublishType() {
        return publishType;
    }

    public String getDockerRegistry() {
        return dockerRegistry;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public String getDockerRegistryUserName() {
        return dockerRegistryUserName;
    }

    public String getDockerRegistryPassword() {
        return dockerRegistryPassword;
    }

    @DataBoundSetter
    public void setSlotName(@CheckForNull String slotName) {
        this.slotName = Util.fixNull(slotName);
    }

    public
    @CheckForNull
    String getSlotName() {
        return slotName;
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
        final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
        final WebApp app = azureClient.webApps().getByResourceGroup(resourceGroup, appService);
        if (app == null) {
            listener.getLogger().println(String.format("App %s in resource group %s not found", appService, resourceGroup));
            return false;
        }

        final String expandedFilePath = build.getEnvironment(listener).expand(filePath);
        final AppServiceDeploymentCommandContext commandContext = new AppServiceDeploymentCommandContext(expandedFilePath, slotName);

        try {
            commandContext.configure(build, listener, app);
        } catch (AzureCloudException e) {
            listener.fatalError(e.getMessage());
            return false;
        }

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
            return "Publish an Azure Web App";
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return new StandardListBoxModel().withAll(
                    CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            ListBoxModel model = new ListBoxModel();
            // list all app service
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }
            if (model.size() == 0) {
                model.add("");
            }
            return model;
        }

        public ListBoxModel doFillAppServiceItems(@QueryParameter final String azureCredentialsId,
                                                  @QueryParameter final String resourceGroup) {
            ListBoxModel model = new ListBoxModel();
            // list all app service
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                for (final WebApp webApp : azureClient.webApps().listByResourceGroup(resourceGroup)) {
                    model.add(webApp.name());
                }
            }
            if (model.size() == 0) {
                model.add("");
            }
            return model;
        }

        @JavaScriptMethod
        public boolean isWebAppOnLinux(final String azureCredentialsId, final String resourceGroup, final String appService) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                WebApp webApp = azureClient.webApps().getByResourceGroup(resourceGroup, appService);
                if (webApp != null) {
                    // todo check the linuxFxVersion
                    // return StringUtils.isNotBlank(webApp.inner().siteConfig().linuxFxVersion()); // not work because siteconfig() return null
                    return webApp.name().contains("linux");
                }
            }

            return false;
        }
    }
}
