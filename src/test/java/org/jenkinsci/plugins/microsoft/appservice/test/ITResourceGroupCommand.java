/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.microsoft.appservice.commands.ResourceGroupCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ITResourceGroupCommand extends IntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ITResourceGroupCommand.class.getName());
    private ResourceGroupCommand rgc = null;
    private ResourceGroupCommand.IResourceGroupCommandData commandDataMock = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        rgc = new ResourceGroupCommand();
        commandDataMock = mock(ResourceGroupCommand.IResourceGroupCommandData.class);
        when(commandDataMock.getResourceGroupName()).thenReturn(testEnv.azureResourceGroup);
        when(commandDataMock.getRegion()).thenReturn(testEnv.azureLocation);
        when(commandDataMock.getAzureServicePrincipal()).thenReturn(servicePrincipal);
        setUpBaseCommandMockErrorHandling(commandDataMock);
    }

    @Test
    public void createNewResourceGroupsWhenMissing() {
        try {
            rgc.execute(commandDataMock);

            Assert.assertNotNull(customTokenCache.getAzureClient().resourceGroups().getByName(testEnv.azureResourceGroup));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void noOpIfResourceGroupAlreadExists() {
        try {
            customTokenCache.getAzureClient().resourceGroups().define(testEnv.azureResourceGroup).withRegion(testEnv.azureLocation);

            rgc.execute(commandDataMock);

            Assert.assertNotNull(customTokenCache.getAzureClient().resourceGroups().getByName(testEnv.azureResourceGroup));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void exceptionIfResourceGroupIfDifferentRegion() {
        try {
            customTokenCache.getAzureClient().resourceGroups().define(testEnv.azureResourceGroup).withRegion(testEnv.azureLocation2).create();

            try {
                rgc.execute(commandDataMock);
            } catch (Exception e) {
                Assert.assertTrue(true);
                return;
            }

            Assert.assertTrue("Excepted exception", false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }
}
