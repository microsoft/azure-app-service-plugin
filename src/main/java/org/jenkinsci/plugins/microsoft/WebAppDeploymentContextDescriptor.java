/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import org.jenkinsci.plugins.microsoft.util.JellyUtil;
import org.kohsuke.stapler.QueryParameter;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class WebAppDeploymentContextDescriptor extends Descriptor<WebAppDeploymentContext> {

    public String defaultLocation() {
        return "West US";
    }

    public String defaultSkuName() {
        return "F1";
    }

    public String defaultSkuCapacity() {
        return "1";
    }

    public String defaultWebappName() {
        return JellyUtil.generateUniqueName("jw");
    }

    public String defaultResourceGroupName() {
        return JellyUtil.generateUniqueName("jw");
    }

    public FormValidation doCheckSkuCapacity(@QueryParameter String value) {
        return JellyUtil.checkForIntValue(value, "Invalid Sku Capacity.");
    }

    public FormValidation doCheckHostingPlanName(@QueryParameter String value) {
        return JellyUtil.checkNotNullValue(value, "Hosting plan name is required.");
    }

    public FormValidation doCheckWebappName(@QueryParameter String value) {
        return JellyUtil.checkNotNullValue(value, "Webapp name is required.");
    }

    public FormValidation doCheckResourceGroupName(@QueryParameter String value) {
        return JellyUtil.checkNotNullValue(value, "Resource group name is required.");
    }

    public FormValidation doCheckFilePath(@QueryParameter String value) {
        return JellyUtil.checkNotNullValue(value, "File path is required.");
    }

    public ListBoxModel doFillLocationItems() {
        ListBoxModel model = new ListBoxModel();
        JellyUtil.addLocationsToModel(model);
        return model;
    }

    public ListBoxModel doFillSkuNameItems() {
        ListBoxModel model = new ListBoxModel();
        model.add("F1");
        model.add("D1");
        model.add("B1");
        model.add("B2");
        model.add("B3");
        model.add("S1");
        model.add("S2");
        model.add("S3");
        model.add("P1");
        model.add("P2");
        model.add("P3");
        model.add("P4");
        return model;
    }

    public FormValidation doVerifyConfiguration(
            @QueryParameter String subscriptionId,
            @QueryParameter String clientId,
            @QueryParameter String clientSecret,
            @QueryParameter String oauth2TokenEndpoint) {
        return JellyUtil.checkSubscriptionInformation(
                subscriptionId,
                clientId,
                clientSecret,
                oauth2TokenEndpoint);
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        // Indicates that this builder can be used with all kinds of project types
        return true;
    }

    public String getDisplayName() {
        return null;
    }
}
