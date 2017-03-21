/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.microsoft.util.JellyUtil;
import org.kohsuke.stapler.DataBoundConstructor;

public class AppService implements Describable<AppService> {

    private final String resourceGroupName;
    private final String appServiceName;
    private final boolean useExistingAppService;
    private final AppServicePlan appServicePlan;

    @DataBoundConstructor
    public AppService(
            final String resourceGroupName,
            final String appServiceName,
            final boolean useExistingAppService,
            final AppServicePlan appServicePlan) {
        this.resourceGroupName = resourceGroupName;
        this.appServiceName = appServiceName;
        this.useExistingAppService = useExistingAppService;
        this.appServicePlan = appServicePlan;
    }

    public AppServicePlan getAppServicePlan() {
        return appServicePlan;
    }

    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    public String getAppServiceName() {
        return this.appServiceName;
    }

    public boolean isUseExistingAppServiceEnabled() {
        return useExistingAppService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<AppService> getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        } else {
            return instance.getDescriptor(getClass());
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AppService> {

        @Override
        public String getDisplayName() {
            return null;
        }

        public String defaultAppServiceName() {
            return JellyUtil.generateUniqueName("jw");
        }

        public String defaultResourceGroupName() {
            return JellyUtil.generateUniqueName("jw");
        }
    }

}
