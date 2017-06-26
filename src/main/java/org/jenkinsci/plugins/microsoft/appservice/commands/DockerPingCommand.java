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
import com.github.dockerjava.api.exception.UnauthorizedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class DockerPingCommand extends DockerCommand {
    public FormValidation ping(final AuthConfig authConfig) {
        DockerClient dockerClient = getDockerClient(authConfig);

        try {
            // make sure local docker is running
            dockerClient.pingCmd().exec();
        } catch (Exception e) {
            return FormValidation.error("Docker is not running on Jenkins master server thus the verification cannot continue. " +
                    "You can proceed to save the configuration. But you need to make sure Docker is properly installed and " +
                    "running on your build agents. The detailed message:" + e.getMessage());
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
        return FormValidation.ok("Docker registry configuration verified. NOTE that you still need make sure docker is " +
                "installed correctly on you build agents.");
    }
}
