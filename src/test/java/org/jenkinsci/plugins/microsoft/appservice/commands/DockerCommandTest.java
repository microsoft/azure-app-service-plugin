/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by juniwang on 27/06/2017.
 */
public class DockerCommandTest extends AbstractDockerCommandTest {

    DockerCommand dockerCommand;

    @Before
    public void setup() {
        dockerCommand = spy(new DockerCommand() {
        });
    }

    @Test
    public void getDockerClientTest() {
        DockerClient dockerClient = dockerCommand.getDockerClient(defaultExampleAuthConfig());
        Assert.assertThat("", dockerClient.authConfig(), equalTo(defaultExampleAuthConfig()));
    }

    @Test
    public void imageAndTagTest() throws AzureCloudException {
        // user-specified image name
        DockerBuildInfo dockerBuildInfo = defaultExampleBuildInfo();
        Assert.assertEquals("someImage:someTag", dockerCommand.imageAndTag(dockerBuildInfo));

        // docker hub, original name is public docker image
        dockerBuildInfo.withDockerImage(null).withLinuxFxVersion("DOCKER|mysql:anyTag").withDockerImageTag("tag1");
        Assert.assertEquals("someUser/mysql:tag1", dockerCommand.imageAndTag(dockerBuildInfo));

        // docker hub, original is private docker image
        dockerBuildInfo.withLinuxFxVersion("DOCKER|someUser/mysql:anyTag").withDockerImageTag("tag2");
        Assert.assertEquals("someUser/mysql:tag2", dockerCommand.imageAndTag(dockerBuildInfo));

        // docker hub, original is private docker image of other people
        dockerBuildInfo.withLinuxFxVersion("DOCKER|anotherUser/mysql:anyTag").withDockerImageTag("tag3");
        Assert.assertEquals("someUser/mysql:tag3", dockerCommand.imageAndTag(dockerBuildInfo));

        // docker hub, original is private with sub path
        dockerBuildInfo.withLinuxFxVersion("DOCKER|anotherUser/sub1/sub2/mysql:anyTag").withDockerImageTag("tag4");
        Assert.assertEquals("someUser/sub1/sub2/mysql:tag4", dockerCommand.imageAndTag(dockerBuildInfo));

        // acr , user specify the image name
        AuthConfig authConfig = defaultExampleAuthConfig().withRegistryAddress("http://someAcr.azurecr.io");
        dockerBuildInfo = defaultExampleBuildInfo().withAuthConfig(authConfig);
        Assert.assertEquals("someImage:someTag", dockerCommand.imageAndTag(dockerBuildInfo));

        // acr, original name is public docker image
        dockerBuildInfo.withDockerImage(null).withLinuxFxVersion("DOCKER|mysql:anyTag").withDockerImageTag("tag1");
        Assert.assertEquals("someAcr.azurecr.io/someUser/mysql:tag1", dockerCommand.imageAndTag(dockerBuildInfo));

        // acr, original is private docker image
        dockerBuildInfo.withLinuxFxVersion("DOCKER|someUser/mysql:anyTag").withDockerImageTag("tag2");
        Assert.assertEquals("someAcr.azurecr.io/someUser/mysql:tag2", dockerCommand.imageAndTag(dockerBuildInfo));

        // acr, original is private docker image of other people
        dockerBuildInfo.withLinuxFxVersion("DOCKER|anotherUser/mysql:anyTag").withDockerImageTag("tag3");
        Assert.assertEquals("someAcr.azurecr.io/someUser/mysql:tag3", dockerCommand.imageAndTag(dockerBuildInfo));

        // acr, original is private with sub path
        dockerBuildInfo.withLinuxFxVersion("DOCKER|anotherUser/sub1/sub2/mysql:anyTag").withDockerImageTag("tag4");
        Assert.assertEquals("someAcr.azurecr.io/someUser/sub1/sub2/mysql:tag4", dockerCommand.imageAndTag(dockerBuildInfo));
    }

    @Test
    public void getRegistryHostTest() throws AzureCloudException {
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("http://someacr.azurecr.io"));
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("http://someacr.azurecr.io/"));
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("https://someacr.azurecr.io"));
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("HTTP://someacr.azurecr.io"));
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("http://someacr.azurecr.io/v1/user"));
        Assert.assertEquals("someacr.azurecr.io",
                dockerCommand.getRegistryHostname("someacr.azurecr.io"));
    }
}
