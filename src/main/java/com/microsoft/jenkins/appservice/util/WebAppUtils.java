/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.util;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.apache.commons.lang.StringUtils;

public final class WebAppUtils {

    private WebAppUtils() {
        // Hide
    }

    public static boolean isJavaApp(WebAppBase app) {
        if (app.javaVersion() != JavaVersion.OFF) {
            // Windows Java App
            return true;
        }

        String linuxFxVersion = app.linuxFxVersion();
        if (StringUtils.isNotBlank(linuxFxVersion) && linuxFxVersion.toLowerCase().contains("jre")) {
            // Linux container Java App
            return true;
        }

        return false;
    }

    public static boolean isBuiltInDockerImage(WebAppBase app) {
        String linuxFxVersion = app.linuxFxVersion();
        return StringUtils.isNotBlank(linuxFxVersion) && !linuxFxVersion.startsWith("DOCKER|");
    }

}
