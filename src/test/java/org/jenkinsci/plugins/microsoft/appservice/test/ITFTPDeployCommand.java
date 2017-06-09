/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.microsoft.appservice.commands.FTPDeployCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ITFTPDeployCommand extends IntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ITFTPDeployCommand.class.getName());
    private FTPDeployCommand command = null;
    private FTPDeployCommand.IFTPDeployCommandData commandDataMock = null;
    private WebApp webApp = null;
    private FilePath workspace = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        command = new FTPDeployCommand();
        commandDataMock = mock(FTPDeployCommand.IFTPDeployCommandData.class);
        setUpBaseCommandMockErrorHandling(commandDataMock);

        // Setup web app
        final ResourceGroup resourceGroup = customTokenCache.getAzureClient().resourceGroups()
                .define(testEnv.azureResourceGroup)
                .withRegion(testEnv.azureLocation)
                .create();
        Assert.assertNotNull(resourceGroup);

        final AppServicePlan asp = customTokenCache.getAzureClient().appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
                .create();
        Assert.assertNotNull(asp);

        webApp = customTokenCache.getAzureClient().appServices().webApps()
                .define(testEnv.appServiceName)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .withExistingAppServicePlan(asp)
                .withJavaVersion(JavaVersion.JAVA_8_NEWEST)
                .withWebContainer(WebContainer.TOMCAT_8_0_NEWEST)
                .create();
        Assert.assertNotNull(webApp);

        final PublishingProfile pubProfile = webApp.getPublishingProfile();
        when(commandDataMock.getPublishingProfile()).thenReturn(pubProfile);

        File workspaceDir = com.google.common.io.Files.createTempDir();
        workspaceDir.deleteOnExit();
        workspace = new FilePath(workspaceDir);

        final AbstractBuild build = mock(AbstractBuild.class);
        when(build.getWorkspace()).thenReturn(workspace);
        when(commandDataMock.getBuild()).thenReturn(build);
    }

    private void setUpWarFile(String path) {
        InputStream sampleApp = getClass().getResourceAsStream("sample-java-app/app.war");
        File warFile = new File(workspace.getRemote(), path);
        warFile.delete();
        File parentDir = warFile.getParentFile();
        parentDir.mkdirs();
        try {
            Files.copy(sampleApp, warFile.toPath());
            warFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void waitForAppReady(URL url, int timeout) throws IllegalStateException, InterruptedException {
        int elapsed = 0;

        while (elapsed < timeout) {
            URLConnection conn = null;
            try {
                conn = url.openConnection();
                InputStream in = conn.getInputStream();
                String content = IOUtils.toString(in, "UTF-8");

                if (content.indexOf("Sample \"Hello, World\" Application") > 0) {
                    LOGGER.info("App is running");
                    return;
                }
            } catch (IOException e) {
                // Ignore and continue waiting
            }

            elapsed++;
            TimeUnit.SECONDS.sleep(1);
        }

        throw new IllegalStateException("App failed to start in timeout");
    }

    /**
     * This test uploads a war file to a non-root directory and verifies web page content
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void uploadNonRoot() throws MalformedURLException, InterruptedException {
        setUpWarFile("webapps/sample.war");
        when(commandDataMock.getFilePath()).thenReturn("webapps/sample.war");

        command.execute(commandDataMock);

        waitForAppReady(new URL("https://" + webApp.defaultHostName() + "/sample/"), 300);
    }

    /**
     * This test uploads a war file to a root directory and verifies web page content
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void uploadRoot() throws MalformedURLException, InterruptedException {
        setUpWarFile("webapps/ROOT.war");
        when(commandDataMock.getFilePath()).thenReturn("webapps/ROOT.war");

        command.execute(commandDataMock);

        waitForAppReady(new URL("https://" + webApp.defaultHostName()), 300);
    }

}
