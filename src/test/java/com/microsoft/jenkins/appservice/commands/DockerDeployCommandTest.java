/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.NameValuePair;
import com.microsoft.azure.management.appservice.WebApp;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerDeployCommandTest {

    @Test
    public void disableSMBShareIfNotSet() {
        // Not set
        Map<String, AppSetting> appSettings = new HashMap<>();
        WebApp.Update update = mock(WebApp.Update.class);
        WebApp webApp = mock(WebApp.class);
        when(webApp.appSettings()).thenReturn(appSettings);

        DockerDeployCommand.disableSMBShareIfNotSet(webApp, update);
        verify(update).withAppSetting(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, "false");

        // Set to true
        appSettings = new HashMap(){
            {
                put(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, "true");
            }
        };
        update = mock(WebApp.Update.class);
        when(webApp.appSettings()).thenReturn(appSettings);

        DockerDeployCommand.disableSMBShareIfNotSet(webApp, update);
        verify(update, never()).withAppSetting(anyString(), anyString());

        // Set to false
        appSettings = new HashMap(){
            {
                put(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, "false");
            }
        };
        update = mock(WebApp.Update.class);
        when(webApp.appSettings()).thenReturn(appSettings);

        DockerDeployCommand.disableSMBShareIfNotSet(webApp, update);
        verify(update, never()).withAppSetting(anyString(), anyString());
    }

    @Test
    public void disableSMBShareIfNotSetSlot() {
        // Not set
        List<NameValuePair> appSettings = new ArrayList<>();
        DockerDeployCommand.disableSMBShareIfNotSet(appSettings);
        Assert.assertEquals("false", appSettings.get(0).value());

        // Set to true
        appSettings = Arrays.asList(
            new NameValuePair().withName(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE).withValue("true")
        );
        DockerDeployCommand.disableSMBShareIfNotSet(appSettings);
        Assert.assertEquals("true", appSettings.get(0).value());

        // Set to false
        appSettings = Arrays.asList(
            new NameValuePair().withName(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE).withValue("false")
        );
        DockerDeployCommand.disableSMBShareIfNotSet(appSettings);
        Assert.assertEquals("false", appSettings.get(0).value());
    }
}
