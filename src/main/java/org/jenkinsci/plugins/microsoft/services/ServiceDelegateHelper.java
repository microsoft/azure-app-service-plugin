/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.services;

//import static com.microsoft.windowsazure.management.configuration.ManagementConfiguration.SUBSCRIPTION_CLOUD_CREDENTIALS;


import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.jenkinsci.plugins.microsoft.util.Constants;
import org.jenkinsci.plugins.microsoft.util.TokenCache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceDelegateHelper {

    private static final Logger LOGGER = Logger.getLogger(ServiceDelegateHelper.class.getName());

    /**
     * Loads configuration object..
     *
     * @param publisher
     * @return
     * @throws AzureCloudException
     */
//    public static Configuration load(IAzureConnectionData publisher)
//            throws AzureCloudException {
//        return ServiceDelegateHelper.loadConfiguration(
//                publisher.getSubscriptionId(),
//                publisher.getClientId(),
//                publisher.getClientSecret(),
//                publisher.getOauth2TokenEndpoint(),
//                Constants.DEFAULT_MANAGEMENT_URL);
//    }

    /**
     * Loads configuration object..
     *
     * @param subscriptionId
     * @param clientId
     * @param clientSecret
     * @param oauth2TokenEndpoint
     * @param serviceManagementURL
     * @return
     * @throws AzureCloudException
     */
//    public static Configuration loadConfiguration(
//            final String subscriptionId,
//            final String clientId,
//            final String clientSecret,
//            final String oauth2TokenEndpoint,
//            final String serviceManagementURL)
//            throws AzureCloudException {
//
//        // Azure libraries are internally using ServiceLoader.load(...) method which uses context classloader and
//        // this causes problems for jenkins plugin, hence setting the class loader explicitly and then reseting back
//        // to original one.
//        ClassLoader thread = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
//
//        try {
//            final Configuration config = TokenCache.getInstance(
//                    subscriptionId, clientId, clientSecret, oauth2TokenEndpoint, serviceManagementURL).get().
//                    getConfiguration();
//
//            LOGGER.log(Level.INFO, "Configuration token: {0}", TokenCloudCredentials.class.cast(
//                    config.getProperty(SUBSCRIPTION_CLOUD_CREDENTIALS)).getToken());
//
//            return config;
//        } finally {
//            Thread.currentThread().setContextClassLoader(thread);
//        }
//    }

    // Gets StorageManagementClient
//    public static StorageManagementClient getStorageManagementClient(final Configuration config) {
//        ClassLoader thread = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
//        TokenCloudCredentials cloudCreds = TokenCloudCredentials.class.cast(
//                config.getProperty(SUBSCRIPTION_CLOUD_CREDENTIALS));
//        try {
//            return StorageManagementService.create(config)
//                    .withRequestFilterFirst(new AzureUserAgentFilter());
//        } finally {
//            Thread.currentThread().setContextClassLoader(thread);
//        }
//    }

    /**
     * Gets ResourceManagementClient.
     *
     * @param config
     * @return
     */
//    public static ResourceManagementClient getResourceManagementClient(final Configuration config) {
//        ClassLoader thread = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
//
//        try {
//            return ResourceManagementService.create(config)
//                    .withRequestFilterFirst(new AzureUserAgentFilter());
//        } finally {
//            Thread.currentThread().setContextClassLoader(thread);
//        }
//    }

    // Gets ManagementClient
//    public static WebSiteManagementClient getWebsiteManagementClient(final Configuration config) {
//        ClassLoader thread = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
//        TokenCloudCredentials cloudCreds = TokenCloudCredentials.class.cast(
//                config.getProperty(SUBSCRIPTION_CLOUD_CREDENTIALS));
//
//        try {
//            TokenCredentials creds = new TokenCredentials(null, cloudCreds.getToken());
//            WebSiteManagementClientImpl client = new WebSiteManagementClientImpl(creds);
//            client.setSubscriptionId(cloudCreds.getSubscriptionId());
//            return client;
//            //.withRequestFilterFirst(new AzureUserAgentFilter());
//        } finally {
//            Thread.currentThread().setContextClassLoader(thread);
//        }
//    }
}
