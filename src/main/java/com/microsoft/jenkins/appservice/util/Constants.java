/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.util;

import com.microsoft.rest.LogLevel;

public final class Constants {

    private Constants() {
        // Hide
    }

    public static final String PLUGIN_NAME = "AzureJenkinsAppService";

    public static final LogLevel DEFAULT_AZURE_SDK_LOGGING_LEVEL = LogLevel.NONE;

    // the first option for select element. Keep the same value as jenkins pre-defined default empty value.
    public static final String EMPTY_SELECTION = "- none -";

    public static final String ZIP_FILE_EXTENSION = "zip";
    public static final String WAR_FILE_EXTENSION = "war";

    public static final String PRODUCTION_SLOT_NAME = "production";

    /**
     * AI constants.
     */
    public static final String AI_WEB_APP = "WebApp";
    public static final String AI_FUNCTIONS = "Functions";
    public static final String AI_START_DEPLOY = "StartDeploy";
    public static final String AI_GIT_DEPLOY = "GitDeploy";
    public static final String AI_GIT_DEPLOY_FAILED = "GitDeployFailed";
    public static final String AI_FTP_DEPLOY = "FTPDeploy";
    public static final String AI_FTP_DEPLOY_FAILED = "GitDeployFailed";
    public static final String AI_WAR_DEPLOY = "WarDeploy";
    public static final String AI_WAR_DEPLOY_FAILED = "WarDeployFailed";
    public static final String AI_ZIP_DEPLOY = "ZipDeploy";
    public static final String AI_ZIP_DEPLOY_FAILED = "ZipDeployFailed";
    public static final String AI_DOCKER_DEPLOY = "DockerDeploy";
    public static final String AI_DOCKER_DEPLOY_FAILED = "DockerDeployFailed";
    public static final String AI_DOCKER_PUSH = "Push";
    public static final String AI_DOCKER_PUSH_FAILED = "PushFailed";
}
