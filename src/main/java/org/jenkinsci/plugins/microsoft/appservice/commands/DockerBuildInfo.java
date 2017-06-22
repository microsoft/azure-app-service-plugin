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

import com.github.dockerjava.api.model.AuthConfig;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;

public class DockerBuildInfo {
    private String dockerfile;
    private String linuxFxVersion; // the original docker image
    private AuthConfig authConfig;
    private String dockerImage;
    private String dockerImageTag;
    private String imageid; // the image Id after build successfully

    public String getLinuxFxVersion() {
        return linuxFxVersion;
    }

    public void setLinuxFxVersion(String linuxFxVersion) {
        this.linuxFxVersion = linuxFxVersion;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public void setDockerImageTag(String dockerImageTag) {
        this.dockerImageTag = dockerImageTag;
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(final AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public String getImageid() {
        return imageid;
    }

    public void setImageid(String imageid) {
        this.imageid = imageid;
    }
}
