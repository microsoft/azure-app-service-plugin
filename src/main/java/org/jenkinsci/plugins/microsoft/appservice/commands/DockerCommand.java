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
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * To provide some common docker-related methods for docker commands
 */
public abstract class DockerCommand {

    protected DockerClient getDockerClient(final DockerBuildInfo dockerBuildInfo) {
        return getDockerClient(dockerBuildInfo.getDockerRegistry(), dockerBuildInfo.getUsername(), dockerBuildInfo.getPassword().getPlainText());
    }

    protected DockerClient getDockerClient(final String registry, final String userName, final String password) {
        final DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(userName)
                .withRegistryPassword(password);
        if (StringUtils.isNotBlank(registry)) {
            builder.withRegistryUrl(registry);
        }

        final DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withConnectTimeout(1000)
                .withMaxTotalConnections(1)
                .withMaxPerRouteConnections(1);

        return DockerClientBuilder.getInstance(builder).withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

    protected String getImageFullName(final DockerBuildInfo dockerBuildInfo) throws AzureCloudException {
        final StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(dockerBuildInfo.getDockerRegistry())) {
            String registry = dockerBuildInfo.getDockerRegistry();
            if (dockerBuildInfo.getDockerRegistry().toLowerCase().startsWith("http")) {
                try {
                    URI uri = new URI(registry);
                    registry = uri.getHost();
                } catch (URISyntaxException e) {
                    throw new AzureCloudException("The docker registry is not a valid URI", e);
                }
            }
            stringBuilder.append(registry).append("/").append(dockerBuildInfo.getDockerImage());
        } else {
            stringBuilder.append(dockerBuildInfo.getDockerImage());
        }
        stringBuilder.append(":").append(dockerBuildInfo.getDockerImageTag());
        return stringBuilder.toString();
    }
}
