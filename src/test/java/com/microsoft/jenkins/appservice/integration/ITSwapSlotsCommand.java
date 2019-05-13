package com.microsoft.jenkins.appservice.integration;


import com.google.common.io.Files;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.jenkins.appservice.commands.FileDeployCommand;
import com.microsoft.jenkins.appservice.commands.SwapSlotsCommand;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ITSwapSlotsCommand extends IntegrationTest {
    private FileDeployCommand command = null;
    private FileDeployCommand.IFileDeployCommandData commandDataMock = null;
    private SwapSlotsCommand swapSlotsCommand = null;
    private SwapSlotsCommand.ISwapSlotsCommandData swapSlotsCommandDataMock = null;
    private WebApp webApp = null;
    private FilePath workspace = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        command = new FileDeployCommand();
        commandDataMock = mock(FileDeployCommand.IFileDeployCommandData.class);
        JobContext jobContextMock = mock(JobContext.class);
        when(commandDataMock.getJobContext()).thenReturn(jobContextMock);
        StreamBuildListener listener = new StreamBuildListener(System.out, Charset.defaultCharset());
        when(commandDataMock.getJobContext().getTaskListener()).thenReturn(listener);
        setUpBaseCommandMockErrorHandling(commandDataMock);

        Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());

        // Setup web app
        final ResourceGroup resourceGroup = azureClient.resourceGroups()
                .define(testEnv.azureResourceGroup)
                .withRegion(testEnv.azureLocation)
                .create();
        Assert.assertNotNull(resourceGroup);

        SkuDescription sd = new SkuDescription();
        sd.withTier("STANDARD");
        sd.withSize("S1");
        PricingTier appServicePricingTier = PricingTier.fromSkuDescription(sd);
        final AppServicePlan asp = azureClient.appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(appServicePricingTier)
                .withOperatingSystem(OperatingSystem.WINDOWS)
                .create();
        Assert.assertNotNull(asp);

        webApp = azureClient.appServices().webApps()
                .define(testEnv.appServiceName)
                .withExistingWindowsPlan(asp)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .withJavaVersion(JavaVersion.JAVA_8_NEWEST)
                .withWebContainer(WebContainer.TOMCAT_8_0_NEWEST)
                .create();
        Assert.assertNotNull(webApp);
        DeploymentSlot deploymentSlot = webApp.deploymentSlots().define("slot")
                .withConfigurationFromParent()
                .create();
        Assert.assertNotNull(deploymentSlot);
        when(commandDataMock.getWebApp()).thenReturn(webApp);


        File workspaceDir = Files.createTempDir();
        workspaceDir.deleteOnExit();
        workspace = new FilePath(workspaceDir);

        final Run run = mock(Run.class);
        when(commandDataMock.getJobContext().getRun()).thenReturn(run);
        when(commandDataMock.getJobContext().getWorkspace()).thenReturn(workspace);

        swapSlotsCommand = new SwapSlotsCommand();
        swapSlotsCommandDataMock = mock(SwapSlotsCommand.ISwapSlotsCommandData.class);
        when(swapSlotsCommandDataMock.getJobContext()).thenReturn(jobContextMock);
        when(swapSlotsCommandDataMock.getJobContext().getTaskListener()).thenReturn(listener);
        setUpBaseCommandMockErrorHandling(swapSlotsCommandDataMock);
        when(swapSlotsCommandDataMock.getWebApp()).thenReturn(webApp);
        when(swapSlotsCommandDataMock.getJobContext().getRun()).thenReturn(run);
        when(swapSlotsCommandDataMock.getJobContext().getWorkspace()).thenReturn(workspace);
        when(swapSlotsCommandDataMock.getSourceSlotName()).thenReturn("slot");
        when(swapSlotsCommandDataMock.getTargetSlotName()).thenReturn("production");
    }


    @Test
    public void testSwapSlots() throws IOException, InterruptedException {
        // setup webapp and slot for swapping
        Utils.extractResourceFile(getClass(), "sample-java-app-zip/gs-spring-boot-0.1.0.zip", workspace.child("gs-spring-boot-0.1.0.zip").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("gs-spring-boot-0.1.0.zip");
        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()), "Greetings from Spring Boot!", 300);

        Utils.extractResourceFile(getClass(), "sample-java-app-zip/gs-spring-boot-0.1.0-slot.zip", workspace.child("gs-spring-boot-0.1.0-slot.zip").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("gs-spring-boot-0.1.0-slot.zip");
        when(commandDataMock.getSlotName()).thenReturn("slot");
        command.execute(commandDataMock);

        DeploymentSlot slot = webApp.deploymentSlots().getByName("slot");
        Utils.waitForAppReady(new URL("https://" + slot.defaultHostName()), "Greetings from Spring Boot Slot!", 300);

        // test swapped slots
        swapSlotsCommand.execute(swapSlotsCommandDataMock);
        Utils.waitForAppReady(new URL("https://" + slot.defaultHostName()), "Greetings from Spring Boot!", 300);
        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()), "Greetings from Spring Boot Slot!", 300);

    }
}
