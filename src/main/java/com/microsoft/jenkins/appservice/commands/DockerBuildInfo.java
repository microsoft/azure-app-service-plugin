/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;

import java.io.Serializable;

public class DockerBuildInfo implements Serializable {
    private String dockerfile;
    private String linuxFxVersion; // the original docker image
    private AuthConfig authConfig;
    private String dockerImage;
    private String dockerImageTag;
    private String imageId; // the image Id after build successfully

    public String getLinuxFxVersion() {
        return linuxFxVersion;
    }

    public DockerBuildInfo withLinuxFxVersion(final String linuxFxVersion) {
        this.linuxFxVersion = linuxFxVersion;
        return this;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public DockerBuildInfo withDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
        return this;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public DockerBuildInfo withDockerImageTag(final String dockerImageTag) {
        this.dockerImageTag = dockerImageTag;
        return this;
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public DockerBuildInfo withDockerfile(final String dockerfile) {
        this.dockerfile = dockerfile;
        return this;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public DockerBuildInfo withAuthConfig(final AuthConfig authConfig) {
        this.authConfig = authConfig;
        return this;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(final String imageId) {
        this.imageId = imageId;
    }
}
