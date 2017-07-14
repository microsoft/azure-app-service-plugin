/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.appservice.util.Constants;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import com.microsoft.jenkins.appservice.commands.DockerBuildInfo;
import com.microsoft.jenkins.appservice.commands.DockerPingCommand;
import com.microsoft.jenkins.appservice.util.TokenCache;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import com.microsoft.jenkins.services.CommandService;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;

public class WebAppDeploymentRecorder extends Recorder implements SimpleBuildStep {

    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String appName;
    private String publishType;
    private String filePath;
    private String dockerImageName;
    private String dockerImageTag;
    private String dockerFilePath;
    private DockerRegistryEndpoint dockerRegistryEndpoint;
    private boolean deployOnlyIfSuccessful;
    private boolean deleteTempImage;

    @CheckForNull
    private
    String sourceDirectory;

    @CheckForNull
    private
    String targetDirectory;

    @CheckForNull
    private
    String slotName;

    @DataBoundConstructor
    public WebAppDeploymentRecorder(
            final String azureCredentialsId,
            final String appName,
            final String resourceGroup) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.appName = appName;
        this.dockerFilePath = "**/Dockerfile";
        this.deployOnlyIfSuccessful = true;
        this.deleteTempImage = true;
    }

    @DataBoundSetter
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    @DataBoundSetter
    public void setPublishType(final String publishType) {
        this.publishType = publishType;
    }

    @DataBoundSetter
    public void setDockerFilePath(final String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }

    @DataBoundSetter
    public void setDockerRegistryEndpoint(final DockerRegistryEndpoint dockerRegistryEndpoint) {
        this.dockerRegistryEndpoint = dockerRegistryEndpoint;
    }

    @DataBoundSetter
    public void setDockerImageName(final String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    @DataBoundSetter
    public void setDockerImageTag(final String dockerImageTag) {
        this.dockerImageTag = dockerImageTag;
    }

    @DataBoundSetter
    public void setDeployOnlyIfSuccessful(final boolean deployOnlyIfSuccessful) {
        this.deployOnlyIfSuccessful = deployOnlyIfSuccessful;
    }

    @DataBoundSetter
    public void setDeleteTempImage(final boolean deleteTempImage) {
        this.deleteTempImage = deleteTempImage;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public DockerRegistryEndpoint getDockerRegistryEndpoint() {
        return dockerRegistryEndpoint;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    public String getAppName() {
        return appName;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getPublishType() {
        return publishType;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public boolean isDeployOnlyIfSuccessful() {
        return deployOnlyIfSuccessful;
    }

    public boolean isDeleteTempImage() {
        return deleteTempImage;
    }

    @DataBoundSetter
    public void setSourceDirectory(@CheckForNull String sourceDirectory) {
        this.sourceDirectory = Util.fixNull(sourceDirectory);
    }

    @CheckForNull
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    @DataBoundSetter
    public void setTargetDirectory(@CheckForNull String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    @CheckForNull
    public String getTargetDirectory() {
        return targetDirectory;
    }

    @DataBoundSetter
    public void setSlotName(@CheckForNull String slotName) {
        this.slotName = Util.fixNull(slotName);
    }

    @CheckForNull
    public String getSlotName() {
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
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        // Only deploy on build succeeds
        // Also check if result is null here because in pipeline web app deploy is not run as a post-build action.
        // In this case result is null and pipeline will stop if previous step failed. So no need to check result in this case.
        if (run.getResult() != null && run.getResult() != Result.SUCCESS && deployOnlyIfSuccessful) {
            listener.getLogger().println("Deploy to Azure Web App is skipped due to previous steps failed.");
            return;
        }

        listener.getLogger().println("Starting Azure Web App Deployment");

        // Get app info
        final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
        final WebApp app = azureClient.webApps().getByResourceGroup(resourceGroup, appName);
        if (app == null) {
            throw new AbortException(String.format("Web App %s in resource group %s not found", appName, resourceGroup));
        }

        final String expandedFilePath = run.getEnvironment(listener).expand(filePath);
        final DockerBuildInfo dockerBuildInfo;
        try {
            dockerBuildInfo = validateDockerBuildInfo(run, listener, app);
        } catch (AzureCloudException e) {
            throw new AbortException(e.getMessage());
        }

        final WebAppDeploymentCommandContext commandContext = new WebAppDeploymentCommandContext(expandedFilePath);
        commandContext.setSourceDirectory(sourceDirectory);
        commandContext.setTargetDirectory(targetDirectory);
        commandContext.setSlotName(slotName);
        commandContext.setPublishType(publishType);
        commandContext.setDockerBuildInfo(dockerBuildInfo);
        commandContext.setDeleteTempImage(deleteTempImage);
        commandContext.setAzureCredentialsId(azureCredentialsId);

        try {
            commandContext.configure(run, workspace, listener, app);
        } catch (AzureCloudException e) {
            throw new AbortException(e.getMessage());
        }

        CommandService.executeCommands(commandContext);

        if (!commandContext.getHasError()) {
            listener.getLogger().println("Done Azure Web App deployment.");
        } else {
            throw new AbortException("Azue Web App deployment failed.");
        }
    }

    private DockerBuildInfo validateDockerBuildInfo(final Run<?, ?> run, final TaskListener listener, final WebApp app)
            throws IOException, InterruptedException, AzureCloudException {
        final DockerBuildInfo dockerBuildInfo = new DockerBuildInfo();

        final String linuxFxVersion = getLinuxFxVersion(app);
        if (StringUtils.isBlank(linuxFxVersion) || isBuiltInDockerImage(linuxFxVersion)) {
            // windows app doesn't need any docker config
            if (StringUtils.isNotBlank(this.publishType) && this.publishType.equals(WebAppDeploymentCommandContext.PUBLISH_TYPE_DOCKER)) {
                throw new AzureCloudException("Publish a windows or built-in image web app through docker is not currently supported.");
            }
            return dockerBuildInfo;
        }

        final EnvVars envVars = run.getEnvironment(listener);

        // docker file
        final String dockerfile = StringUtils.isBlank(dockerFilePath) ? "**/Dockerfile" : dockerFilePath;
        dockerBuildInfo.withDockerfile(envVars.expand(dockerfile));

        // AuthConfig for registry
        dockerBuildInfo.withAuthConfig(getAuthConfig(run.getParent(), dockerRegistryEndpoint));

        // the original docker image on Azure
        dockerBuildInfo.withLinuxFxVersion(linuxFxVersion);

        // docker image tag
        final String tag = StringUtils.isBlank(dockerImageTag)
                ? String.valueOf(run.getNumber()) : envVars.expand(dockerImageTag);
        dockerBuildInfo.withDockerImageTag(tag);

        // docker image name
        final String imageName = StringUtils.isBlank(dockerImageName) ? "" : envVars.expand(dockerImageName);
        dockerBuildInfo.withDockerImage(imageName);

        return dockerBuildInfo;
    }


    private static AuthConfig getAuthConfig(final Item project, final DockerRegistryEndpoint endpoint) throws AzureCloudException {
        if (endpoint == null || StringUtils.isBlank(endpoint.getCredentialsId())) {
            throw new AzureCloudException("docker registry configuration is not valid");
        }

        return getAuthConfig(endpoint.getUrl(), endpoint.getToken(project));
    }

    private static AuthConfig getAuthConfig(final String registryAddress, final DockerRegistryToken dockerRegistryToken)
            throws AzureCloudException {
        if (dockerRegistryToken == null) {
            throw new AzureCloudException("cannot find the docker registry credential.");
        }

        final AuthConfig authConfig = new AuthConfig();

        // formulated registry address
        String url = StringUtils.isBlank(registryAddress) ? AuthConfig.DEFAULT_SERVER_ADDRESS : registryAddress;
        if (!url.toLowerCase().matches("^\\w+://.*")) {
            url = "http://" + registryAddress;
        }
        authConfig.withRegistryAddress(url);

        // registry credential
        final String[] credentials = new String(Base64.decodeBase64(dockerRegistryToken.getToken()), Charsets.UTF_8)
                .split(":", 2);
        authConfig.withUsername(credentials[0]);
        authConfig.withPassword(credentials[1]);

        return authConfig;
    }

    public static final String getLinuxFxVersion(final WebApp webApp) throws AzureCloudException {
        // https://github.com/Azure/azure-sdk-for-java/issues/1761
        // access the field via reflection for now
        try {
            final SiteConfigResourceInner siteConfig = (SiteConfigResourceInner) FieldUtils.readField(webApp, "siteConfig", true);
            if (siteConfig != null) {
                return siteConfig.linuxFxVersion();
            }
        } catch (IllegalAccessException e) {
            throw new AzureCloudException(String.format("Cannot get the docker container info of web app %s", webApp.name()));
        }
        return "";
    }

    public static boolean isBuiltInDockerImage(final String linuxFxVersion) {
        return !linuxFxVersion.startsWith("DOCKER|");
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("azureWebAppPublish")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Publish an Azure Web App";
        }

        public DockerRegistryEndpoint.DescriptorImpl getDockerRegistryEndpointDescriptor() {
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return (DockerRegistryEndpoint.DescriptorImpl)
                        jenkins.getDescriptor(DockerRegistryEndpoint.class);
            } else {
                return null;
            }
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel();
            model.add(Constants.EMPTY_SELECTION, "");
            // list all app service
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }
            return model;
        }

        public ListBoxModel doFillAppNameItems(@QueryParameter final String azureCredentialsId,
                                               @QueryParameter final String resourceGroup) {
            final ListBoxModel model = new ListBoxModel();
            model.add(Constants.EMPTY_SELECTION, "");
            // list all app service
            // https://github.com/Azure/azure-sdk-for-java/issues/1762
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                final PagedList<SiteInner> list = azureClient.webApps().inner().listByResourceGroup(resourceGroup);
                list.loadAll();
                for (final SiteInner webApp : list) {
                    model.add(webApp.name());
                }
            }
            return model;
        }

        public FormValidation doVerifyConfiguration(@AncestorInPath final Item owner,
                                                    @QueryParameter final String url,
                                                    @QueryParameter final String credentialsId) {

            final DockerPingCommand pingCommand = new DockerPingCommand();
            try {
                IdCredentials idCredentials = null;
                for (IdCredentials credential : CredentialsProvider.lookupCredentials(
                        IdCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList())) {
                    if (credential.getId().equalsIgnoreCase(credentialsId)) {
                        idCredentials = credential;
                        break;
                    }
                }
                if (idCredentials == null) {
                    return FormValidation.error("credential cannot be found");
                }
                final DockerRegistryToken token = AuthenticationTokens.convert(DockerRegistryToken.class, idCredentials);
                final AuthConfig authConfig = getAuthConfig(url, token);
                return pingCommand.ping(authConfig);
            } catch (AzureCloudException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        @JavaScriptMethod
        public boolean isWebAppOnLinux(final String azureCredentialsId, final String resourceGroup, final String appName) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                final SiteConfigResourceInner siteConfig = azureClient.webApps().inner().getConfiguration(resourceGroup, appName);
                if (siteConfig != null) {
                    return StringUtils.isNotBlank(siteConfig.linuxFxVersion())
                            && !isBuiltInDockerImage(siteConfig.linuxFxVersion());
                }
            }
            return false;
        }
    }
}
