/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.github.dockerjava.api.model.AuthConfig;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.appservice.commands.DockerBuildInfo;
import com.microsoft.jenkins.appservice.commands.DockerPingCommand;
import com.microsoft.jenkins.appservice.util.AzureUtils;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;

public class WebAppDeploymentRecorder extends BaseDeploymentRecorder {

    private String publishType;
    private String dockerImageName;
    private String dockerImageTag;
    private String dockerFilePath;
    private DockerRegistryEndpoint dockerRegistryEndpoint;
    private boolean deleteTempImage;

    @CheckForNull
    private
    String slotName;

    @DataBoundConstructor
    public WebAppDeploymentRecorder(
            final String azureCredentialsId,
            final String appName,
            final String resourceGroup) {
        super(azureCredentialsId, resourceGroup, appName);
        this.dockerFilePath = "**/Dockerfile";
        this.deleteTempImage = true;
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

    public String getPublishType() {
        return publishType;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public boolean isDeleteTempImage() {
        return deleteTempImage;
    }

    @DataBoundSetter
    public void setSlotName(@CheckForNull final String slotName) {
        this.slotName = Util.fixNull(slotName);
    }

    @CheckForNull
    public String getSlotName() {
        return slotName;
    }

    @Override
    public void perform(
            @Nonnull final Run<?, ?> run,
            @Nonnull final FilePath workspace,
            @Nonnull final Launcher launcher,
            @Nonnull final TaskListener listener) throws InterruptedException, IOException {
        // Only deploy on build succeeds
        // Also check if result is null here because in pipeline web app deploy is not run as a post-build action.
        // In this case result is null and pipeline will stop if previous step failed. So no need to check result in
        // this case.
        if (run.getResult() != null && run.getResult() != Result.SUCCESS && isDeployOnlyIfSuccessful()) {
            listener.getLogger().println("Deploy to Azure Web App is skipped due to previous steps failed.");
            return;
        }

        listener.getLogger().println("Starting Azure Web App Deployment");

        // Get app info
        final String azureCredentialsId = getAzureCredentialsId();
        final Azure azureClient = AzureUtils.buildAzureClient(AzureCredentials.getServicePrincipal(azureCredentialsId));
        final String resourceGroup = getResourceGroup();
        final String appName = getAppName();
        final WebApp app = azureClient.webApps().getByResourceGroup(resourceGroup, appName);
        if (app == null) {
            throw new AbortException(String.format("Web App %s in resource group %s not found",
                    appName, resourceGroup));
        }

        final String expandedFilePath = run.getEnvironment(listener).expand(getFilePath());
        final DockerBuildInfo dockerBuildInfo;
        try {
            dockerBuildInfo = validateDockerBuildInfo(run, listener, app);
        } catch (AzureCloudException e) {
            throw new AbortException(e.getMessage());
        }

        final WebAppDeploymentCommandContext commandContext = new WebAppDeploymentCommandContext(expandedFilePath);
        commandContext.setSourceDirectory(getSourceDirectory());
        commandContext.setTargetDirectory(getTargetDirectory());
        commandContext.setSlotName(slotName);
        commandContext.setPublishType(publishType);
        commandContext.setDockerBuildInfo(dockerBuildInfo);
        commandContext.setDeleteTempImage(deleteTempImage);
        commandContext.setAzureCredentialsId(azureCredentialsId);

        try {
            commandContext.configure(run, workspace, launcher, listener, app);
        } catch (AzureCloudException e) {
            throw new AbortException(e.getMessage());
        }

        commandContext.executeCommands();

        if (!commandContext.getLastCommandState().isError()) {
            listener.getLogger().println("Done Azure Web App deployment.");
        } else {
            throw new AbortException("Azure Web App deployment failed.");
        }
    }

    private DockerBuildInfo validateDockerBuildInfo(Run<?, ?> run, TaskListener listener, WebApp app)
            throws IOException, InterruptedException, AzureCloudException {
        final DockerBuildInfo dockerBuildInfo = new DockerBuildInfo();

        final String linuxFxVersion = app.linuxFxVersion();
        if (StringUtils.isBlank(linuxFxVersion) || isBuiltInDockerImage(linuxFxVersion)) {
            // windows app doesn't need any docker config
            if (StringUtils.isNotBlank(this.publishType)
                    && this.publishType.equals(WebAppDeploymentCommandContext.PUBLISH_TYPE_DOCKER)) {
                throw new AzureCloudException(
                        "Publish a windows or built-in image web app through docker is not currently supported.");
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


    private static AuthConfig getAuthConfig(final Item project, final DockerRegistryEndpoint endpoint)
            throws AzureCloudException {
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

    public static boolean isBuiltInDockerImage(final String linuxFxVersion) {
        return !linuxFxVersion.startsWith("DOCKER|");
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("azureWebAppPublish")
    public static final class DescriptorImpl extends BaseDeploymentRecorder.DescriptorImpl {

        @Override
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

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath final Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            return listResourceGroupItems(azureCredentialsId);
        }

        public ListBoxModel doFillAppNameItems(@QueryParameter final String azureCredentialsId,
                                               @QueryParameter final String resourceGroup) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = AzureUtils.buildAzureClient(
                        AzureCredentials.getServicePrincipal(azureCredentialsId));
                return listAppNameItems(azureClient.webApps(), resourceGroup);
            } else {
                return new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            }
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
                final DockerRegistryToken token = AuthenticationTokens.convert(
                        DockerRegistryToken.class, idCredentials);
                final AuthConfig authConfig = getAuthConfig(url, token);
                return pingCommand.ping(authConfig);
            } catch (AzureCloudException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        @JavaScriptMethod
        public boolean isWebAppOnLinux(
                final String azureCredentialsId,
                final String resourceGroup,
                final String appName) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = AzureUtils.buildAzureClient(
                        AzureCredentials.getServicePrincipal(azureCredentialsId));
                final SiteConfigResourceInner siteConfig =
                        azureClient.webApps().inner().getConfiguration(resourceGroup, appName);
                if (siteConfig != null) {
                    return StringUtils.isNotBlank(siteConfig.linuxFxVersion())
                            && !isBuiltInDockerImage(siteConfig.linuxFxVersion());
                }
            }
            return false;
        }
    }
}
