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

    public DockerBuildInfo withLinuxFxVersion(final String aLinuxFxVersion) {
        this.linuxFxVersion = aLinuxFxVersion;
        return this;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public DockerBuildInfo withDockerImage(final String aDockerImage) {
        this.dockerImage = aDockerImage;
        return this;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public DockerBuildInfo withDockerImageTag(final String aDockerImageTag) {
        this.dockerImageTag = aDockerImageTag;
        return this;
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public DockerBuildInfo withDockerfile(final String aDockerfile) {
        this.dockerfile = aDockerfile;
        return this;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public DockerBuildInfo withAuthConfig(final AuthConfig aAuthConfig) {
        this.authConfig = aAuthConfig;
        return this;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(final String aImageId) {
        this.imageId = aImageId;
    }
}
