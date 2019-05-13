package com.microsoft.jenkins.appservice;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.appservice.util.AzureUtils;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.NoSuchElementException;

public class WebAppSwapSlotsRecorder extends Recorder implements SimpleBuildStep {
    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String appName;
    private final String sourceSlotName;
    private final String targetSlotName;

    @DataBoundConstructor
    public WebAppSwapSlotsRecorder(String azureCredentialsId,
                                   String resourceGroup,
                                   String appName,
                                   String sourceSlotName,
                                   String targetSlotName) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.appName = appName;
        this.sourceSlotName = sourceSlotName;
        this.targetSlotName = targetSlotName;
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

    public String getSourceSlotName() {
        return sourceSlotName;
    }

    public String getTargetSlotName() {
        return targetSlotName;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        final Azure azureClient = AzureUtils.buildClient(run.getParent(), azureCredentialsId);
        WebApp app;
        try {
            app = azureClient.webApps().getByResourceGroup(resourceGroup, appName);
        } catch (NoSuchElementException e) {
            throw new AbortException(String.format("Web App %s in resource group %s not found",
                    appName, resourceGroup));
        }
        if (app == null) {
            throw new AbortException(String.format("Web App %s in resource group %s not found",
                    appName, resourceGroup));
        }

        WebAppSwapSlotsCommandContext context = new WebAppSwapSlotsCommandContext();
        context.setSourceSlotName(sourceSlotName);
        context.setTargetSlotName(targetSlotName);
        context.setWebApp(app);

        try {
            context.configure(run, workspace, launcher, listener, app);
        } catch (AzureCloudException e) {
            throw new AbortException(e.getMessage());
        }

        context.executeCommands();

        if (!context.getLastCommandState().isError()) {
            listener.getLogger().println(String.format("Done swapping Azure Web "
                    + "App deployment slots between %s and %s.", sourceSlotName, targetSlotName));
        } else {
            throw new AbortException("Azure Web App swapping slots failed.");
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    @Symbol("azureWebAppSwapSlots")
    public static final class DescriptorImpl extends BaseDeploymentRecorder.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Swap slots for an Azure Web App";
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath final Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@AncestorInPath Item owner,
                                                     @QueryParameter String azureCredentialsId) {
            return listResourceGroupItems(owner, azureCredentialsId);
        }


        public ListBoxModel doFillAppNameItems(@AncestorInPath Item owner,
                                               @QueryParameter String azureCredentialsId,
                                               @QueryParameter String resourceGroup) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                return listAppNameItems(azureClient.webApps(), resourceGroup);
            } else {
                return new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            }
        }

        public FormValidation doCheckTargetSlotName(@QueryParameter String sourceSlotName,
                                                    @QueryParameter String targetSlotName) {
            if (StringUtils.isBlank(sourceSlotName) || StringUtils.isBlank(targetSlotName)) {
                return FormValidation.ok();
            }
            if (sourceSlotName.equals(targetSlotName)) {
                return FormValidation.error("Target slot cannot be the same as source slot.");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillSourceSlotNameItems(@AncestorInPath Item owner,
                                                      @QueryParameter String azureCredentialsId,
                                                      @QueryParameter String resourceGroup,
                                                      @QueryParameter String appName) {
            return getSlots(owner, azureCredentialsId, resourceGroup, appName);
        }

        public ListBoxModel doFillTargetSlotNameItems(@AncestorInPath Item owner,
                                                      @QueryParameter String azureCredentialsId,
                                                      @QueryParameter String resourceGroup,
                                                      @QueryParameter String appName) {
            return getSlots(owner, azureCredentialsId, resourceGroup, appName);
        }

        private ListBoxModel getSlots(Item owner, String azureCredentialsId,
                                      String resourceGroup, String appName) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)
                    && StringUtils.isNotBlank(appName)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                WebApp webApp = azureClient.webApps().getByResourceGroup(resourceGroup, appName);
                PagedList<DeploymentSlot> deploymentSlots = webApp.deploymentSlots().list();
                ListBoxModel model = new ListBoxModel();
                model.add(Constants.PRODUCTION_SLOT_NAME);
                for (DeploymentSlot slot : deploymentSlots) {
                    model.add(slot.name());
                }
                return model;
            } else {
                return new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            }

        }
    }
}
