/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.NameParser;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureCredentials;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.jenkinsci.plugins.microsoft.appservice.commands.DockerBuildInfo;
import org.jenkinsci.plugins.microsoft.appservice.commands.DockerPingCommand;
import org.jenkinsci.plugins.microsoft.appservice.util.Constants;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.jenkinsci.plugins.microsoft.services.CommandService;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AppServiceDeploymentRecorder extends Recorder {

    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String appService;
    private String publishType;
    private String filePath;
    private String dockerImageName;
    private String dockerImageTag;
    private String dockerFilePath;
    private DockerRegistryEndpoint dockerRegistryEndpoint;
    private boolean deployOnlyIfSuccessful;
    private boolean deleteTempImage;

    private
    @CheckForNull
    String sourceDirectory;
    private
    @CheckForNull
    String targetDirectory;
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

    public String getAppService() {
        return appService;
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

    public
    @CheckForNull
    String getSourceDirectory() {
        return sourceDirectory;
    }

    @DataBoundSetter
    public void setTargetDirectory(@CheckForNull String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    public
    @CheckForNull
    String getTargetDirectory() {
        return targetDirectory;
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
        // only deploy on build succeeds
        if (build.getResult() != Result.SUCCESS && deployOnlyIfSuccessful) {
            listener.getLogger().println("Deploy to Azure app service: SKIPPED.");
            return false;
        }

        listener.getLogger().println("Starting Azure App Service Deployment");

        // Get app info
        final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
        final WebApp app = azureClient.webApps().getByResourceGroup(resourceGroup, appService);
        if (app == null) {
            listener.getLogger().println(String.format("App %s in resource group %s not found", appService, resourceGroup));
            return false;
        }

        final String expandedFilePath = build.getEnvironment(listener).expand(filePath);
        final DockerBuildInfo dockerBuildInfo;
        try {
            dockerBuildInfo = validateDockerBuildInfo(build, listener, app);
        } catch (AzureCloudException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        }
        final AppServiceDeploymentCommandContext commandContext = new AppServiceDeploymentCommandContext(expandedFilePath);
        commandContext.setSourceDirectory(sourceDirectory);
        commandContext.setTargetDirectory(targetDirectory);
        commandContext.setSlotName(slotName);
        commandContext.setPublishType(publishType);
        commandContext.setDockerBuildInfo(dockerBuildInfo);

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

    private DockerBuildInfo validateDockerBuildInfo(final AbstractBuild<?, ?> build, final BuildListener listener, final WebApp app)
            throws IOException, InterruptedException, AzureCloudException {
        final DockerBuildInfo dockerBuildInfo = new DockerBuildInfo();

        final String linuxFxVersion = getLinuxFxVersion(app);
        if (StringUtils.isBlank(linuxFxVersion) || isBuiltInDockerImage(linuxFxVersion)) {
            // windows app doesn't need any docker config
            if (this.publishType.equals(AppServiceDeploymentCommandContext.PUBLISH_TYPE_DOCKER)) {
                throw new AzureCloudException("Publish a windows or built-in image web app through docker is not currently supported.");
            }
            return dockerBuildInfo;
        }

        final EnvVars envVars = build.getEnvironment(listener);

        // docker file
        final String dockerfile = StringUtils.isBlank(dockerFilePath) ? "**/Dockerfile" : dockerFilePath;
        dockerBuildInfo.withDockerfile(envVars.expand(dockerfile));

        // AuthConfig for registry
        dockerBuildInfo.withAuthConfig(getAuthConfig(build.getParent(), dockerRegistryEndpoint));

        // the original docker image on Azure
        dockerBuildInfo.withLinuxFxVersion(linuxFxVersion);

        // docker image tag
        final String tag = StringUtils.isBlank(dockerImageTag) ?
                String.valueOf(build.getNumber()) : envVars.expand(dockerImageTag);
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
            throw new AzureCloudException(String.format("Cannot get the dcoker container info of web app %s", webApp.name()));
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
            } else
                return null;
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return new StandardListBoxModel().withAll(
                    CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel();
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
            final ListBoxModel model = new ListBoxModel();
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
            if (model.size() == 0) {
                model.add("");
            }
            return model;
        }

        public final FormValidation doVerifyConfiguration(final @AncestorInPath Item owner,
                                                          final @QueryParameter String url,
                                                          final @QueryParameter String credentialsId) {

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
        public boolean isWebAppOnLinux(final String azureCredentialsId, final String resourceGroup, final String appService) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                final SiteConfigResourceInner siteConfig = azureClient.webApps().inner().getConfiguration(resourceGroup, appService);
                if (siteConfig != null) {
                    return StringUtils.isNotBlank(siteConfig.linuxFxVersion())
                            && !isBuiltInDockerImage(siteConfig.linuxFxVersion());
                }
            }
            return false;
        }
    }
}
