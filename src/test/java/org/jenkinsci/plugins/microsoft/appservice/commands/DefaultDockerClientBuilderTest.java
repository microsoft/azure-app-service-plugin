/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import org.junit.Assert;
import org.junit.Test;

public class DefaultDockerClientBuilderTest extends AbstractDockerCommandTest {

    @Test
    public void build() {
        DockerClientBuilder builder = new DefaultDockerClientBuilder();
        DockerClient dockerClient = builder.build(defaultExampleAuthConfig());
        Assert.assertEquals(defaultExampleAuthConfig(), dockerClient.authConfig());
    }

}
