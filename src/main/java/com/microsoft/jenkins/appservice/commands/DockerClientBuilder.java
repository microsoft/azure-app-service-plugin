/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;

public interface DockerClientBuilder {

    DockerClient build(AuthConfig authConfig);

}
