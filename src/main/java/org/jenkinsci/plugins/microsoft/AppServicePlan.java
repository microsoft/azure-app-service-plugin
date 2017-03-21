/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class AppServicePlan implements Describable<AppServicePlan> {

    private final String appServicePlanName;
    private final String pricingTier;
    private final String region;
    private boolean createAppServicePlan;

    @DataBoundConstructor
    public AppServicePlan(
            final String appServicePlanName,
            final boolean createAppServicePlan,
            final String pricingTier,
            final String region) {
        this.appServicePlanName = appServicePlanName;
        this.createAppServicePlan = createAppServicePlan;
        this.pricingTier = pricingTier;
        this.region = region;
    }

    public String getAppServicePlanName() {
        return this.appServicePlanName;
    }

    public boolean isCreateAppServicePlanEnabled() {
        return this.createAppServicePlan;
    }

    public String getPricingTier() {
        return this.pricingTier;
    }

    public String getRegion() {
        return this.region;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<AppServicePlan> getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        } else {
            return instance.getDescriptor(getClass());
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AppServicePlan> {

        @Override
        public String getDisplayName() {
            return null;
        }

        public String defaultRegion() {
            return "West US";
        }

        public ListBoxModel doFillRegionItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("West US");
            model.add("East US");
            model.add("North Central US");
            model.add("South Central US");
            model.add("North Europe");
            model.add("West Europe");
            model.add("East Asia");
            model.add("Southeast Asia");
            model.add("Japan East");
            model.add("Japan West");
            model.add("Brazil South");
            return model;
        }

        public ListBoxModel doFillPricingTierItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("Free_F1");
            model.add("Shared_D1");
            model.add("Basic_B1");
            model.add("Basic_B2");
            model.add("Basic_B3");
            model.add("Standard_S1");
            model.add("Standard_S2");
            model.add("Standard_S3");
            model.add("Premium_P1");
            model.add("Premium_P2");
            model.add("Premium_P3");
            model.add("Premium_P4");
            return model;
        }

        public String defaultPricingTier() {
            return "Free_F1";
        }
    }
}
