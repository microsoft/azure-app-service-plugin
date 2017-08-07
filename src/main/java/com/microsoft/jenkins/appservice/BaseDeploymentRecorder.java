/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.model.HasInner;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.appservice.util.TokenCache;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import java.util.Collections;

public abstract class BaseDeploymentRecorder extends Recorder implements SimpleBuildStep {

    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String appName;

    @CheckForNull private String filePath;
    @CheckForNull private String sourceDirectory;
    @CheckForNull private String targetDirectory;
    private boolean deployOnlyIfSuccessful;

    protected BaseDeploymentRecorder(
            final String azureCredentialsId,
            final String resourceGroup,
            final String appName) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.appName = appName;
        this.deployOnlyIfSuccessful = true;
    }

    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    @DataBoundSetter
    public void setFilePath(@CheckForNull final String filePath) {
        this.filePath = Util.fixNull(filePath);
    }

    public String getFilePath() {
        return filePath;
    }

    @DataBoundSetter
    public void setSourceDirectory(@CheckForNull final String sourceDirectory) {
        this.sourceDirectory = Util.fixNull(sourceDirectory);
    }

    @CheckForNull
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    @DataBoundSetter
    public void setTargetDirectory(@CheckForNull final String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    @CheckForNull
    public String getTargetDirectory() {
        return targetDirectory;
    }

    @DataBoundSetter
    public void setDeployOnlyIfSuccessful(final boolean deployOnlyIfSuccessful) {
        this.deployOnlyIfSuccessful = deployOnlyIfSuccessful;
    }

    public boolean isDeployOnlyIfSuccessful() {
        return deployOnlyIfSuccessful;
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
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    protected static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        protected ListBoxModel listAzureCredentialsIdItems(final Item owner) {
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }

        protected ListBoxModel listResourceGroupItems(final String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            // list all resource groups
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = TokenCache.getInstance(
                        AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }
            return model;
        }

        protected ListBoxModel listAppNameItems(final HasInner<WebAppsInner> hasInner,
                                                final String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            // list all app services
            // https://github.com/Azure/azure-sdk-for-java/issues/1762
            for (final SiteInner webApp : hasInner.inner().listByResourceGroup(resourceGroup)) {
                model.add(webApp.name());
            }
            return model;
        }
    }

}
