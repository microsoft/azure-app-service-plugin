/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.util;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebAppUtilsTest {

    @Test
    public void isJavaApp() {
        WebAppBase app = mock(WebAppBase.class);
        when(app.javaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        Assert.assertTrue(WebAppUtils.isJavaApp(app));

        when(app.javaVersion()).thenReturn(JavaVersion.OFF);
        when(app.linuxFxVersion()).thenReturn(null);
        Assert.assertFalse(WebAppUtils.isJavaApp(app));

        when(app.linuxFxVersion()).thenReturn("TOMCAT|8.5-jre8");
        Assert.assertTrue(WebAppUtils.isJavaApp(app));

        when(app.linuxFxVersion()).thenReturn("PHP|5.6");
        Assert.assertFalse(WebAppUtils.isJavaApp(app));
    }

    @Test
    public void isBuiltInDockerImage() {
        WebAppBase app = mock(WebAppBase.class);
        when(app.linuxFxVersion()).thenReturn(null);
        Assert.assertFalse(WebAppUtils.isBuiltInDockerImage(app));

        when(app.linuxFxVersion()).thenReturn("DOCKER|nginx");
        Assert.assertFalse(WebAppUtils.isBuiltInDockerImage(app));

        when(app.linuxFxVersion()).thenReturn("PHP|5.6");
        Assert.assertTrue(WebAppUtils.isBuiltInDockerImage(app));
    }
}
