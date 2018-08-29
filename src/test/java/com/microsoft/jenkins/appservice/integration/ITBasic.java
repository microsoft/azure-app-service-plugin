package com.microsoft.jenkins.appservice.integration;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.NoSuchElementException;

public class ITBasic extends IntegrationTest {

    @Test(expected = NoSuchElementException.class)
    public void noWebAppExists() {
        Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());
        azureClient.webApps().getByResourceGroup("not-exist", "not-exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void noSlotExists() {
        Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());

        final AppServicePlan asp = azureClient.appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
                .withOperatingSystem(OperatingSystem.WINDOWS)
                .create();
        Assert.assertNotNull(asp);

        WebApp webApp = azureClient.appServices().webApps()
                .define(testEnv.appServiceName)
                .withExistingWindowsPlan(asp)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .withJavaVersion(JavaVersion.JAVA_8_NEWEST)
                .withWebContainer(WebContainer.TOMCAT_8_0_NEWEST)
                .create();
        Assert.assertNotNull(webApp);
        webApp.deploymentSlots().getByName("not-exist");
    }
}
