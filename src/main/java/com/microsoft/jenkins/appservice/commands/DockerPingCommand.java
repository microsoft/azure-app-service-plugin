/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.UnauthorizedException;
import com.github.dockerjava.api.model.AuthConfig;
import hudson.util.FormValidation;

public class DockerPingCommand {
    public FormValidation ping(final AuthConfig authConfig) {
        DockerClient dockerClient = new DefaultDockerClientBuilder().build(authConfig);

        try {
            // make sure local docker is running
            dockerClient.pingCmd().exec();
        } catch (Exception e) {
            return FormValidation.error("Docker is not running on Jenkins master server thus the verification cannot continue. "
                    + "You can proceed to save the configuration. But you need to make sure Docker is properly installed and "
                    + "running on your build agents. The detailed message:" + e.getMessage());
        }


        try {
            // validate the remote docker registry
            dockerClient.authCmd().withAuthConfig(authConfig).exec();
        } catch (UnauthorizedException un) {
            return FormValidation.error(String.format("Unauthorized access to %s: incorrect username or password.",
                    authConfig.getRegistryAddress()));
        } catch (Exception e) {
            return FormValidation.error("Validation fails: " + e.getMessage());
        }
        return FormValidation.ok("Docker registry configuration verified. NOTE that you still need make sure docker is "
                + "installed correctly on you build agents.");
    }
}
