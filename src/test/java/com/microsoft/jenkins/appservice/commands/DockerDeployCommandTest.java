/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerDeployCommandTest {

    private void assertDisableSMBShareIfNotSet(
            WebAppBase.Update update, Map<String, AppSetting> appSettings, boolean shouldSet) {
        WebAppBase webApp = mock(WebAppBase.class);
        when(webApp.getAppSettings()).thenReturn(appSettings);

        DockerDeployCommand.disableSMBShareIfNotSet(webApp, update);

        if (shouldSet) {
            verify(update).withAppSetting(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, "false");
        } else {
            verify(update, never()).withAppSetting(anyString(), anyString());
        }
    }

    private <T extends WebAppBase.Update> void disableSMBShareIfNotSet(Class<T> clazz) {
        // Not set
        assertDisableSMBShareIfNotSet(mock(clazz), new HashMap<String, AppSetting>(), true);

        // Set to true
        assertDisableSMBShareIfNotSet(mock(clazz), new HashMap<String, AppSetting>() {
            {
                AppSetting appSetting = mock(AppSetting.class);
                when(appSetting.key()).thenReturn(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE);
                when(appSetting.value()).thenReturn("true");
                put(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, appSetting);
            }
        }, false);

        // Set to false
        assertDisableSMBShareIfNotSet(mock(clazz), new HashMap<String, AppSetting>() {
            {
                AppSetting appSetting = mock(AppSetting.class);
                when(appSetting.key()).thenReturn(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE);
                when(appSetting.value()).thenReturn("false");
                put(DockerDeployCommand.SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, appSetting);
            }
        }, false);
    }

    @Test
    public void disableSMBShareIfNotSet() {
        disableSMBShareIfNotSet(WebApp.Update.class);
        disableSMBShareIfNotSet(DeploymentSlot.Update.class);
    }
}
