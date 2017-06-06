/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.management.appservice.AppServicePricingTier;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import org.jenkinsci.plugins.microsoft.appservice.AppServiceDeploymentCommandContext;
import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        final PublishingProfile pubProfile = mock(PublishingProfile.class);
        when(pubProfile.ftpUrl()).thenReturn("ftp://example.com");
        when(pubProfile.ftpUsername()).thenReturn("user");
        when(pubProfile.ftpPassword()).thenReturn("pass");

        ctx.setPublishingProfile(pubProfile);

        Assert.assertEquals("ftp://example.com", ctx.getPublishingProfile().ftpUrl());
        Assert.assertEquals("user", ctx.getPublishingProfile().ftpUsername());
        Assert.assertEquals("pass", ctx.getPublishingProfile().ftpPassword());

        final AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        final BuildListener listener = mock(BuildListener.class);
        ctx.configure(build, listener);

        Assert.assertEquals(DeploymentState.Running, ctx.getDeploymentState());
    }
}
