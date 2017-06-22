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

    protected String imageAndTag(final DockerBuildInfo dockerBuildInfo) {
        return String.format("%s:%s", dockerBuildInfo.getDockerImage(), dockerBuildInfo.getDockerImageTag());
    }

    protected boolean isDockerHub(final AuthConfig authConfig) {
        return StringUtils.isBlank(authConfig.getRegistryAddress()) ||
                AuthConfig.DEFAULT_SERVER_ADDRESS.equalsIgnoreCase(authConfig.getRegistryAddress());
    }

    protected String imageAndTagAndRegistry(final DockerBuildInfo dockerBuildInfo)
            throws AzureCloudException {
        final AuthConfig authConfig = dockerBuildInfo.getAuthConfig();
        if (isDockerHub(authConfig)) {
            return imageAndTag(dockerBuildInfo);
        } else {
            try {
                final URI uri = new URI(authConfig.getRegistryAddress());
                final String registry = uri.getHost();
                return String.format("%s/%s", registry, imageAndTag(dockerBuildInfo));
            } catch (URISyntaxException e) {
                throw new AzureCloudException("The docker registry is not a valid URI", e);
            }
        }
    }
}
