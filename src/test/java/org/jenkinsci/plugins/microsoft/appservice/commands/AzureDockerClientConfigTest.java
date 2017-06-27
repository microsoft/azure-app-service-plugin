/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.Assert;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;

/**
 * Created by juniwang on 26/06/2017.
 */
public class AzureDockerClientConfigTest {


    public static final AzureDockerClientConfig EXAMPLE_CONFIG = newExampleConfig();

    private static AzureDockerClientConfig newExampleConfig() {

        String dockerCertPath = dockerCertPath();

        return new AzureDockerClientConfig(URI.create("tcp://foo"), "dockerConfig", "apiVersion", "registryUrl", "registryUsername", "registryPassword", "registryEmail",
                new LocalDirectorySSLConfig(dockerCertPath));
    }

    private static String homeDir() {
        return "target/test-classes/someHomeDir";
    }

    private static String dockerCertPath() {
        return homeDir() + "/docker";
    }

    @Test
    public void equals() throws Exception {
        Assert.assertEquals(EXAMPLE_CONFIG, newExampleConfig());
    }

    @Test
    public void environmentDockerHost() throws Exception {

        // given docker host in env
        Map<String, String> env = new HashMap<String, String>();
        env.put(AzureDockerClientConfig.DOCKER_HOST, "tcp://baz:8768");
        // and it looks to be SSL disabled
        env.remove("DOCKER_CERT_PATH");

        // given default cert path
        Properties systemProperties = new Properties();
        systemProperties.setProperty("user.name", "someUserName");
        systemProperties.setProperty("user.home", homeDir());

        // when you build a config
        AzureDockerClientConfig config = buildConfig(env, systemProperties);

        Assert.assertEquals(config.getDockerHost(), URI.create("tcp://baz:8768"));
    }

    @Test
    public void environment() throws Exception {

        // given a default config in env properties
        Map<String, String> env = new HashMap<String, String>();
        env.put(AzureDockerClientConfig.DOCKER_HOST, "tcp://foo");
        env.put(AzureDockerClientConfig.API_VERSION, "apiVersion");
        env.put(AzureDockerClientConfig.REGISTRY_USERNAME, "registryUsername");
        env.put(AzureDockerClientConfig.REGISTRY_PASSWORD, "registryPassword");
        env.put(AzureDockerClientConfig.REGISTRY_EMAIL, "registryEmail");
        env.put(AzureDockerClientConfig.REGISTRY_URL, "registryUrl");
        env.put(AzureDockerClientConfig.DOCKER_CONFIG, "dockerConfig");
        env.put(AzureDockerClientConfig.DOCKER_CERT_PATH, dockerCertPath());
        env.put(AzureDockerClientConfig.DOCKER_TLS_VERIFY, "1");

        // when you build a config
        AzureDockerClientConfig config = buildConfig(env, new Properties());

        // then we get the example object
        Assert.assertEquals(config, EXAMPLE_CONFIG);
    }

    private AzureDockerClientConfig buildConfig(Map<String, String> env, Properties systemProperties) {
        return AzureDockerClientConfig.createDefaultConfigBuilder(env, systemProperties).build();
    }

    @Test
    public void defaults() throws Exception {

        // given default cert path
        Properties systemProperties = new Properties();
        systemProperties.setProperty("user.name", "someUserName");
        systemProperties.setProperty("user.home", homeDir());

        // when you build config
        AzureDockerClientConfig config = buildConfig(Collections.<String, String> emptyMap(), systemProperties);

        // then the cert path is as expected
        Assert.assertEquals(config.getDockerHost(), URI.create("unix:///var/run/docker.sock"));
        Assert.assertEquals(config.getRegistryUsername(), "someUserName");
        Assert.assertEquals(config.getRegistryUrl(), AuthConfig.DEFAULT_SERVER_ADDRESS);
        Assert.assertEquals(config.getApiVersion(), RemoteApiVersion.unknown());
        Assert.assertEquals(config.getDockerConfig(), homeDir() + "/.docker");
        Assert.assertNull(config.getSSLConfig());
    }

    @Test
    public void systemProperties() throws Exception {

        // given system properties based on the example
        Properties systemProperties = new Properties();
        systemProperties.put(AzureDockerClientConfig.DOCKER_HOST, "tcp://foo");
        systemProperties.put(AzureDockerClientConfig.API_VERSION, "apiVersion");
        systemProperties.put(AzureDockerClientConfig.REGISTRY_USERNAME, "registryUsername");
        systemProperties.put(AzureDockerClientConfig.REGISTRY_PASSWORD, "registryPassword");
        systemProperties.put(AzureDockerClientConfig.REGISTRY_EMAIL, "registryEmail");
        systemProperties.put(AzureDockerClientConfig.REGISTRY_URL, "registryUrl");
        systemProperties.put(AzureDockerClientConfig.DOCKER_CONFIG, "dockerConfig");
        systemProperties.put(AzureDockerClientConfig.DOCKER_CERT_PATH, dockerCertPath());
        systemProperties.put(AzureDockerClientConfig.DOCKER_TLS_VERIFY, "1");

        // when you build new config
        AzureDockerClientConfig config = buildConfig(Collections.<String, String> emptyMap(), systemProperties);

        // then it is the same as the example
        Assert.assertEquals(config, EXAMPLE_CONFIG);

    }

    @Test
    public void serializableTest() {
        final byte[] serialized = SerializationUtils.serialize(EXAMPLE_CONFIG);
        final AzureDockerClientConfig deserialized = (AzureDockerClientConfig) SerializationUtils.deserialize(serialized);

        Assert.assertThat("Deserialized object mush match source object", deserialized, equalTo(EXAMPLE_CONFIG));
    }

    @Test()
    public void testSslContextEmpty() throws Exception {
        new AzureDockerClientConfig(URI.create("tcp://foo"), "dockerConfig", "apiVersion", "registryUrl", "registryUsername", "registryPassword", "registryEmail",
                null);
    }



    @Test()
    public void testTlsVerifyAndCertPath() throws Exception {
        new AzureDockerClientConfig(URI.create("tcp://foo"), "dockerConfig", "apiVersion", "registryUrl", "registryUsername", "registryPassword", "registryEmail",
                new LocalDirectorySSLConfig(dockerCertPath()));
    }


    @Test()
    public void testTcpHostScheme() throws Exception {
        new AzureDockerClientConfig(URI.create("tcp://foo"), "dockerConfig", "apiVersion", "registryUrl", "registryUsername", "registryPassword", "registryEmail",
                null);
    }

    @Test()
    public void testUnixHostScheme() throws Exception {
        new AzureDockerClientConfig(URI.create("unix://foo"), "dockerConfig", "apiVersion", "registryUrl", "registryUsername", "registryPassword", "registryEmail",
                null);
    }

}