/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.util.AzureCredentials;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.microsoft.appservice.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.appservice.util.TokenCache;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IntegrationTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();
    @Rule
    public Timeout globalTimeout = Timeout.seconds(20 * 60); // integration tests are very slow
    private static final Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());

    protected static class TestEnvironment {

        public final String subscriptionId;
        public final String clientId;
        public final String clientSecret;
        public final String oauth2TokenEndpoint;
        public final String serviceManagementURL;
        public final String authenticationEndpoint;
        public final String resourceManagerEndpoint;
        public final String graphEndpoint;
        public final Region azureLocation;
        public final Region azureLocation2;
        public final String azureResourceGroup;
        public final Map<String, String> blobEndpointSuffixForTemplate;
        public final Map<String, String> blobEndpointSuffixForCloudStorageAccount;
        public final static String AZUREPUBLIC = "azure public";
        public final static String AZURECHINA = "azure china";
        public final static String AZUREUSGOVERMENT = "azure us goverment";
        public final static String AZUREGERMAN = "azure german";

        TestEnvironment() {
            subscriptionId = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_SUBSCRIPTION_ID");
            clientId = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_CLIENT_ID");
            clientSecret = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_CLIENT_SECRET");
            oauth2TokenEndpoint = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_TOKEN_ENDPOINT");
            serviceManagementURL = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_AZURE_URL");
            authenticationEndpoint = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_AZURE_AUTH_URL");
            resourceManagerEndpoint = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_AZURE_RESOURCE_URL");
            graphEndpoint = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_AZURE_GRAPH_URL");

            azureLocation = Region.fromName(TestEnvironment.loadFromEnv("APP_SERVICE_TEST_DEFAULT_LOCATION", "East US"));
            azureLocation2 = Region.fromName(TestEnvironment.loadFromEnv("APP_SERVICE_TEST_DEFAULT_LOCATION", "West Central US"));
            azureResourceGroup = TestEnvironment.loadFromEnv("APP_SERVICE_TEST_DEFAULT_RESOURCE_GROUP_PREFIX", "webapp-tst") + "-" + TestEnvironment.GenerateRandomString(16);

            blobEndpointSuffixForTemplate = new HashMap<>();
            blobEndpointSuffixForTemplate.put(AZUREPUBLIC, ".blob.core.windows.net/");
            blobEndpointSuffixForTemplate.put(AZURECHINA, ".blob.core.chinacloudapi.cn/");
            blobEndpointSuffixForTemplate.put(AZUREUSGOVERMENT, ".blob.core.usgovcloudapi.net/");
            blobEndpointSuffixForTemplate.put(AZUREGERMAN, ".blob.core.cloudapi.de/");
            blobEndpointSuffixForCloudStorageAccount = new HashMap<>();
            blobEndpointSuffixForCloudStorageAccount.put(AZUREPUBLIC, "core.windows.net/");
            blobEndpointSuffixForCloudStorageAccount.put(AZURECHINA, "core.chinacloudapi.cn/");
            blobEndpointSuffixForCloudStorageAccount.put(AZUREUSGOVERMENT, "core.usgovcloudapi.net/");
            blobEndpointSuffixForCloudStorageAccount.put(AZUREGERMAN, "core.cloudapi.de/");
        }

        private static String loadFromEnv(final String name) {
            return TestEnvironment.loadFromEnv(name, "");
        }

        private static String loadFromEnv(final String name, final String defaultValue) {
            final String value = System.getenv(name);
            if (value == null || value.isEmpty()) {
                return defaultValue;
            } else {
                return value;
            }
        }

        public static String GenerateRandomString(int length) {
            String uuid = UUID.randomUUID().toString();
            return uuid.replaceAll("[^a-z0-9]", "a").substring(0, length);
        }
    }

    protected TokenCache customTokenCache = null;
    protected TestEnvironment testEnv = null;
    protected AzureCredentials.ServicePrincipal servicePrincipal = null;

    @Before
    public void setUp() {
        testEnv = new TestEnvironment();
        LOGGER.log(Level.INFO, "=========================== {0}", testEnv.azureResourceGroup);
        servicePrincipal = new AzureCredentials.ServicePrincipal(
                testEnv.subscriptionId,
                testEnv.clientId,
                testEnv.clientSecret,
                testEnv.oauth2TokenEndpoint,
                testEnv.serviceManagementURL,
                testEnv.authenticationEndpoint,
                testEnv.resourceManagerEndpoint,
                testEnv.graphEndpoint);
        customTokenCache = TokenCache.getInstance(servicePrincipal);
        clearAzureResources();
    }

    @After
    public void tearDown() {
        clearAzureResources();
    }

    protected void clearAzureResources() {
        try {
            customTokenCache.getAzureClient().resourceGroups().deleteByName(testEnv.azureResourceGroup);
        } catch (CloudException e) {
            if (e.response().code() != 404) {
                LOGGER.log(Level.SEVERE, null, e);
            }
        }

    }

    protected void setUpBaseCommandMockErrorHandling(IBaseCommandData commandDataMock) {

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                throw (Exception) args[0];
            }
        }).when(commandDataMock).logError(any(Exception.class));

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Exception ex = (Exception) args[1];
                String msg = (String) args[0];
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                throw new Exception(msg + ex.getMessage() + "\n" + sw.toString());
            }
        }).when(commandDataMock).logError(anyString(), any(Exception.class));

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String msg = (String) args[0];
                throw new Exception(msg);
            }
        }).when(commandDataMock).logError(anyString());
    }
}
