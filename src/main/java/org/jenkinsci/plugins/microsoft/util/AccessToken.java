/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.util;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public class AccessToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String subscriptionId;

    private final String serviceManagementUrl;

    private final String token;

    private final long expiration;

    AccessToken(
            final String subscriptionId, final String serviceManagementUrl, final AuthenticationResult authres) {
        this.subscriptionId = subscriptionId;
        this.serviceManagementUrl = serviceManagementUrl;
        this.token = authres.getAccessToken();
        this.expiration = authres.getExpiresOn();
    }

    public Configuration getConfiguration() throws AzureCloudException {
        try {
            return ManagementConfiguration.configure(
                    null,
                    new URI(serviceManagementUrl),
                    subscriptionId,
                    token);
        } catch (URISyntaxException e) {
            throw new AzureCloudException("The syntax of the Url in the publish settings file is incorrect.", e);
        } catch (IOException e) {
            throw new AzureCloudException("Error updating authentication configuration", e);
        }
    }

    public Date getExpirationDate() {
        return new Date(expiration);
    }

    public boolean isExpiring() {
        return expiration < System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return token;
    }
}
