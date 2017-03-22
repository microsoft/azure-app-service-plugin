/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

public class AzureAuth implements Describable<AzureAuth> {

    private final String azureCredentialsId;

    @DataBoundConstructor
    public AzureAuth(final String azureCredentialsId) {
        this.azureCredentialsId = azureCredentialsId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<AzureAuth> getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        } else {
            return instance.getDescriptor(getClass());
        }
    }

    public String getAzureCredentialsId() {
        return this.azureCredentialsId;
    }

    public AzureCredentials.ServicePrincipal getServicePrincipal() {
        return AzureCredentials.getServicePrincipal(azureCredentialsId);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AzureAuth> {

        @Override
        public String getDisplayName() {
            return null;
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return new StandardListBoxModel().withAll(
                    CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }
    }
}
