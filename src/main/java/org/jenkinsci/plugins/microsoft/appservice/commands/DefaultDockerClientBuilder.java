/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.apache.commons.lang.SystemUtils;

import java.io.Serializable;

public class DefaultDockerClientBuilder implements DockerClientBuilder, Serializable {

    private static final int CONNECT_TIMEOUT = 1000;
    private static final int MAX_TOTAL_CONNECTIONS = 1;
    private static final int MAX_PER_ROUTE_CONNECTIONS = 1;
    private static final String DEFAULT_DOCKER_HOST_ON_WINDOWS = "tcp://localhost:2375";


    @Override
    public DockerClient build(AuthConfig authConfig) {
        final AzureDockerClientConfig.Builder builder = AzureDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(authConfig.getUsername())
                .withRegistryPassword(authConfig.getPassword())
                .withRegistryUrl(authConfig.getRegistryAddress())
                .withRegistryEmail(authConfig.getEmail());

        // must enable tcp on windows by check the option "Expose daemon on tcp://localhost:2375 without TLS"
        // more reading at https://docs.microsoft.com/en-us/virtualization/windowscontainers/manage-docker/configure-docker-daemon
        if (SystemUtils.IS_OS_WINDOWS) {
            builder.withDockerHost(DEFAULT_DOCKER_HOST_ON_WINDOWS);
        }

        final DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withMaxTotalConnections(MAX_TOTAL_CONNECTIONS)
                .withMaxPerRouteConnections(MAX_PER_ROUTE_CONNECTIONS);

        return com.github.dockerjava.core.DockerClientBuilder.getInstance(builder.build())
                .withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

}
