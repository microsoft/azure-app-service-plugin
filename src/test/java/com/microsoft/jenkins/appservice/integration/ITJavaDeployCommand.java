/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.integration;

import com.google.common.io.Files;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.jenkins.appservice.commands.FileDeployCommand;
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

public class ITJavaDeployCommand extends IntegrationTest {
    private FileDeployCommand command = null;
    private FileDeployCommand.IFileDeployCommandData commandDataMock = null;
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

        final AppServicePlan asp = azureClient.appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
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
        when(commandDataMock.getWebApp()).thenReturn(webApp);


        File workspaceDir = Files.createTempDir();
        workspaceDir.deleteOnExit();
        workspace = new FilePath(workspaceDir);

        final Run run = mock(Run.class);
        when(commandDataMock.getJobContext().getRun()).thenReturn(run);
        when(commandDataMock.getJobContext().getWorkspace()).thenReturn(workspace);
    }

    /**
     * This test uploads a normal war file to deploy java app and verifies web page content
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void warDeployNonRoot() throws IOException, InterruptedException {
        Utils.extractResourceFile(getClass(), "sample-java-app/app.war", workspace.child("webapps/sample.war").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("webapps/sample.war");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName() + "/sample/"), "Sample \"Hello, World\" Application", 300);
    }

    /**
     * This test uploads a root war file to deploy java app and verifies web page content
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void warDeployRoot() throws IOException, InterruptedException {
        Utils.extractResourceFile(getClass(), "sample-java-app/app.war", workspace.child("webapps/ROOT.war").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("webapps/ROOT.war");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()), "Sample \"Hello, World\" Application", 300);
    }

    /**
     *  This test uploads a zip file to deploy java app and verifies web page content
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void zipDeploy() throws IOException, InterruptedException {
        Utils.extractResourceFile(getClass(), "sample-java-app-zip/gs-spring-boot-0.1.0.zip", workspace.child("gs-spring-boot-0.1.0.zip").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("*.zip");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()), "Greetings from Spring Boot!", 300);
    }
}
