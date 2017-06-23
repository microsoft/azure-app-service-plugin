/*
 Copyright 2017 Microsoft Open Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.NameParser;
import com.github.dockerjava.core.SSLConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.AbstractBuild;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * To provide some common docker-related methods for docker commands
 */
public abstract class DockerCommand {

    protected DockerClient getDockerClient(final AuthConfig authConfig) {
        final AzureDockerClientConfig.Builder builder = AzureDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(authConfig.getUsername())
                .withRegistryPassword(authConfig.getPassword())
                .withRegistryUrl(authConfig.getRegistryAddress())
                .withRegistryEmail(authConfig.getEmail());

        final DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withConnectTimeout(1000)
                .withMaxTotalConnections(1)
                .withMaxPerRouteConnections(1);

        return DockerClientBuilder.getInstance(builder.build())
                .withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

    protected String getFullImageName(final DockerBuildInfo dockerBuildInfo) throws AzureCloudException {
        if (StringUtils.isNotBlank(dockerBuildInfo.getDockerImage()))
            return dockerBuildInfo.getDockerImage();

        final String linuxFxVersion = dockerBuildInfo.getLinuxFxVersion();
        // the linuxFxVersion should be "DOCKER|<registry>/repo:tag"
        // <registry>/repo:tag
        final String originalImageName = linuxFxVersion.substring(linuxFxVersion.indexOf("|") + 1).toLowerCase();

        final String newRegistry = getRegistryHostname(dockerBuildInfo.getAuthConfig().getRegistryAddress());
        final StringBuilder stringBuilder = new StringBuilder();
        if (!newRegistry.contains("index.docker.io")) {
            // append the registry host as part of the image name, it's required for non-dockerHub registry
            stringBuilder.append(newRegistry).append("/");
        }
        // append user name
        stringBuilder.append(dockerBuildInfo.getAuthConfig().getUsername()).append("/");
        // append the original repo part after username
        final NameParser.ReposTag reposTag = NameParser.parseRepositoryTag(originalImageName);
        final String imageNameWithoutTagAndRegistry = NameParser.resolveRepositoryName(reposTag.repos).reposName;
        final String[] nameParts = imageNameWithoutTagAndRegistry.split("/", 2);
        if (nameParts.length == 1) {
            stringBuilder.append(imageNameWithoutTagAndRegistry);
        } else {
            stringBuilder.append(nameParts[1]);
        }

        return stringBuilder.toString();
    }

    protected String imageAndTag(final DockerBuildInfo dockerBuildInfo) throws AzureCloudException {
        return String.format("%s:%s", getFullImageName(dockerBuildInfo), dockerBuildInfo.getDockerImageTag());
    }

    protected boolean isDockerHub(final AuthConfig authConfig) {
        return StringUtils.isBlank(authConfig.getRegistryAddress()) ||
                AuthConfig.DEFAULT_SERVER_ADDRESS.equalsIgnoreCase(authConfig.getRegistryAddress());
    }

    private String getRegistryHostname(String registryAddress) throws AzureCloudException {
        try {
            if (!registryAddress.toLowerCase().matches("^\\w+://.*")) {
                registryAddress = "http://" + registryAddress;
            }

            final URI uri = new URI(registryAddress);
            return uri.getHost();
        } catch (URISyntaxException e) {
            throw new AzureCloudException("The docker registry is not a valid URI", e);
        }
    }
}
