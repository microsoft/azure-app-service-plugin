/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.microsoft.appservice.commands.CreateWebAppCommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ITCreateWebAppCommand extends IntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ITCreateWebAppCommand.class.getName());
    private CreateWebAppCommand createAppServiceCommand = null;
    private CreateWebAppCommand.ICreateWebAppCommandData commandDataMock = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        createAppServiceCommand = new CreateWebAppCommand();
        commandDataMock = mock(CreateWebAppCommand.ICreateWebAppCommandData.class);
        when(commandDataMock.getResourceGroupName()).thenReturn(testEnv.azureResourceGroup);
        when(commandDataMock.getAzureServicePrincipal()).thenReturn(servicePrincipal);
        when(commandDataMock.getAppServiceName()).thenReturn(testEnv.appServiceName);
        when(commandDataMock.getAppServicePlanName()).thenReturn(testEnv.appServicePlanName);
        when(commandDataMock.getRegion()).thenReturn(testEnv.azureLocation);
        when(commandDataMock.getAppServicePricingTier()).thenReturn(testEnv.appServicePricingTier);
        setUpBaseCommandMockErrorHandling(commandDataMock);
    }

    @Test
    public void noOpIfWebAppServiceAlreadyExists() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(true);
            AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                    .define(testEnv.appServicePlanName)
                    .withRegion(testEnv.azureLocation)
                    .withNewResourceGroup(testEnv.azureResourceGroup)
                    .withPricingTier(testEnv.appServicePricingTier)
                    .create();
            Assert.assertNotNull(asp);
            WebApp webApp = customTokenCache.getAzureClient().appServices().webApps()
                    .define(testEnv.appServiceName)
                    .withExistingResourceGroup(testEnv.azureResourceGroup)
                    .withExistingAppServicePlan(asp)
                    .create();
            Assert.assertNotNull(webApp);

            createAppServiceCommand.execute(commandDataMock);

            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
            WebApp webApp2 = customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName);
            Assert.assertNotNull(webApp2);
            Assert.assertTrue(webApp2.appServicePlanId().equalsIgnoreCase(asp.id()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void createWebAppWithExistingAppServicePlan() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(false);
            when(commandDataMock.useExistingAppServicePlan()).thenReturn(true);
            AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                    .define(testEnv.appServicePlanName)
                    .withRegion(testEnv.azureLocation)
                    .withNewResourceGroup(testEnv.azureResourceGroup)
                    .withPricingTier(testEnv.appServicePricingTier)
                    .create();
            Assert.assertNotNull(asp);

            createAppServiceCommand.execute(commandDataMock);

            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
            WebApp webApp = customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName);
            Assert.assertNotNull(webApp);
            Assert.assertTrue(webApp.appServicePlanId().equalsIgnoreCase(asp.id()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void createNewWebAppWithExistingWebApp() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(false);
            when(commandDataMock.useExistingAppServicePlan()).thenReturn(true);
            AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                    .define(testEnv.appServicePlanName)
                    .withRegion(testEnv.azureLocation)
                    .withNewResourceGroup(testEnv.azureResourceGroup)
                    .withPricingTier(testEnv.appServicePricingTier)
                    .create();
            Assert.assertNotNull(asp);
            WebApp webApp = customTokenCache.getAzureClient().appServices().webApps()
                    .define(testEnv.appServiceName)
                    .withExistingResourceGroup(testEnv.azureResourceGroup)
                    .withExistingAppServicePlan(asp)
                    .create();
            Assert.assertNotNull(webApp);

            createAppServiceCommand.execute(commandDataMock);

            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
            WebApp webApp2 = customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName);
            Assert.assertNotNull(webApp);
            Assert.assertTrue(webApp2.id().equalsIgnoreCase(webApp.id()));
            Assert.assertTrue(webApp2.appServicePlanId().equalsIgnoreCase(asp.id()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void createWebAppWithMissingAppServicePlanFails() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(false);
            when(commandDataMock.useExistingAppServicePlan()).thenReturn(true);

            createAppServiceCommand.execute(commandDataMock);

        } catch (Exception e) {
            if (e instanceof CommandErrorException) {
                //the command logs an error so we get here
                verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.UnSuccessful);
                Assert.assertNull(customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName));
                Assert.assertNull(customTokenCache.getAzureClient().appServices().appServicePlans().getByGroup(testEnv.azureResourceGroup, testEnv.appServicePlanName));
            } else {
                LOGGER.log(Level.SEVERE, null, e);
                Assert.assertTrue(e.getMessage(), false);
            }
        }
    }

    @Test
    public void createWebAppWithNewAppServicePlan() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(false);
            when(commandDataMock.useExistingAppServicePlan()).thenReturn(false);
            //the command expects that the resource group already exists
            customTokenCache.getAzureClient().resourceGroups().define(testEnv.azureResourceGroup).withRegion(testEnv.azureLocation).create();

            createAppServiceCommand.execute(commandDataMock);

            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
            WebApp webApp = customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName);
            Assert.assertNotNull(webApp);
            final AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans().getById(webApp.appServicePlanId());

            Assert.assertNotNull(asp);
            Assert.assertTrue(asp.name().equalsIgnoreCase(testEnv.appServicePlanName));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void createWebAppWithNewAppServicePlanThatExists() {
        try {
            when(commandDataMock.useExistingAppService()).thenReturn(false);
            when(commandDataMock.useExistingAppServicePlan()).thenReturn(false);
            AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                    .define(testEnv.appServicePlanName)
                    .withRegion(testEnv.azureLocation)
                    .withNewResourceGroup(testEnv.azureResourceGroup)
                    .withPricingTier(testEnv.appServicePricingTier)
                    .create();
            Assert.assertNotNull(asp);

            createAppServiceCommand.execute(commandDataMock);

            verify(commandDataMock, times(1)).setDeploymentState(DeploymentState.Success);
            WebApp webApp = customTokenCache.getAzureClient().appServices().webApps().getByGroup(testEnv.azureResourceGroup, testEnv.appServiceName);
            Assert.assertNotNull(webApp);
            Assert.assertTrue(webApp.appServicePlanId().equalsIgnoreCase(asp.id()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }
}
