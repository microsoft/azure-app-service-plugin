/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.microsoft.appservice.util.JellyUtil;
import org.kohsuke.stapler.DataBoundConstructor;

public class AppService implements Describable<AppService> {

    private final String resourceGroupName;
    private final String appServiceName;

    @DataBoundConstructor
    public AppService(
            final String resourceGroupName,
            final String appServiceName) {
        this.resourceGroupName = resourceGroupName;
        this.appServiceName = appServiceName;
    }

    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    public String getAppServiceName() {
        return this.appServiceName;
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
