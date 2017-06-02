/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.AppServicePricingTier;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.BuildListener;
import org.jenkinsci.plugins.microsoft.appservice.AppServiceDeploymentCommandContext;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class AppServiceDeploymentCommandContextTest {

    @Test
    public void getterSetter() {
        final AzureCredentials.ServicePrincipal servicePrincipal = mock(AzureCredentials.ServicePrincipal.class);
        AppServiceDeploymentCommandContext ctx = new AppServiceDeploymentCommandContext(
            servicePrincipal,
            "resourceGroup",
            Region.ASIA_EAST,
            "appService",
            "appServicePlan",
            "FREE_F1",
            "sample.war",
            true,
            true
        );

        Assert.assertEquals("resourceGroup", ctx.getResourceGroupName());
        Assert.assertEquals(Region.ASIA_EAST, ctx.getRegion());
        Assert.assertEquals("appService", ctx.getAppServiceName());
        Assert.assertEquals("appServicePlan", ctx.getAppServicePlanName());
        Assert.assertEquals(AppServicePricingTier.FREE_F1, ctx.getAppServicePricingTier());
        Assert.assertEquals("sample.war", ctx.getFilePath());
        Assert.assertTrue(ctx.useExistingAppService());
        Assert.assertTrue(ctx.useExistingAppServicePlan());
        Assert.assertFalse(ctx.getHasError());
        Assert.assertFalse(ctx.getIsFinished());
        Assert.assertEquals(DeploymentState.Unknown, ctx.getDeploymentState());

        ctx.setFTPUrl("ftp://example.com");
        ctx.setFTPUserName("user");
        ctx.setFTPPassword("pass");

        Assert.assertEquals("ftp://example.com", ctx.getFTPUrl());
        Assert.assertEquals("user", ctx.getFTPUserName());
        Assert.assertEquals("pass", ctx.getFTPPassword());

        final BuildListener listener = mock(BuildListener.class);
        ctx.configure(listener);

        Assert.assertEquals(DeploymentState.Running, ctx.getDeploymentState());
    }
}
