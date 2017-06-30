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

import java.io.Serializable;

public class DefaultDockerClientBuilder implements DockerClientBuilder, Serializable {

    private static final int CONNECT_TIMEOUT = 1000;
    private static final int MAX_TOTAL_CONNECTIONS = 1;
    private static final int MAX_PER_ROUTE_CONNECTIONS = 1;

    @Override
    public DockerClient build(AuthConfig authConfig) {
        final AzureDockerClientConfig.Builder builder = AzureDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(authConfig.getUsername())
                .withRegistryPassword(authConfig.getPassword())
                .withRegistryUrl(authConfig.getRegistryAddress())
                .withRegistryEmail(authConfig.getEmail());

        final DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withMaxTotalConnections(MAX_TOTAL_CONNECTIONS)
                .withMaxPerRouteConnections(MAX_PER_ROUTE_CONNECTIONS);

        return com.github.dockerjava.core.DockerClientBuilder.getInstance(builder.build())
                .withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

}
