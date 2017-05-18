/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.appservice.commands.GetPublishSettingsCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class ITGetPublishSettingsCommand extends IntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ITGetPublishSettingsCommand.class.getName());
    private GetPublishSettingsCommand command = null;
    private GetPublishSettingsCommand.IGetPublishSettingsCommandData commandDataMock = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        command = new GetPublishSettingsCommand();
        commandDataMock = mock(GetPublishSettingsCommand.IGetPublishSettingsCommandData.class);
        when(commandDataMock.getResourceGroupName()).thenReturn(testEnv.azureResourceGroup);
        when(commandDataMock.getAppServiceName()).thenReturn(testEnv.appServiceName);
        when(commandDataMock.getAzureServicePrincipal()).thenReturn(servicePrincipal);
        setUpBaseCommandMockErrorHandling(commandDataMock);
    }

    @Test
    public void getExistingApp() {
        final AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
                .create();
        Assert.assertNotNull(asp);
        final WebApp webApp = customTokenCache.getAzureClient().appServices().webApps()
                .define(testEnv.appServiceName)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .withExistingAppServicePlan(asp)
                .create();
        Assert.assertNotNull(webApp);
        final PublishingProfile pubProfile = webApp.getPublishingProfile();

        command.execute(commandDataMock);

        verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
        verify(commandDataMock, times(1)).setFTPUrl(pubProfile.ftpUrl());
        verify(commandDataMock, times(1)).setFTPUserName(pubProfile.ftpUsername());
        verify(commandDataMock, times(1)).setFTPPassword(pubProfile.ftpPassword());
    }

    @Test
    public void getAppThatNotExist() {
        try {
            command.execute(commandDataMock);

            Assert.fail("Should throw an exception but not");
        } catch (Exception e) {
            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.HasError);
        }
    }
}
