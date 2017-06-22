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
import com.github.dockerjava.api.model.AuthConfig;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class DockerPingCommand extends DockerCommand {
    public void ping(final AuthConfig authConfig)
            throws AzureCloudException {
        DockerClient dockerClient = getDockerClient(authConfig);
        try {
            dockerClient.pingCmd().exec();
        } catch (Exception e) {
            throw new AzureCloudException("Cannot connect to docker for:" + e.getMessage(), e);
        }
    }
}
