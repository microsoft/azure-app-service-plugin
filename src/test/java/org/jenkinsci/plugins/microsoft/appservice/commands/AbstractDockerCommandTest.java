package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.github.dockerjava.api.model.AuthConfig;

/**
 * Created by juniwang on 27/06/2017.
 */
public class AbstractDockerCommandTest {

    protected AuthConfig defaultExampleAuthConfig() {
        return new AuthConfig()
                .withRegistryAddress(AuthConfig.DEFAULT_SERVER_ADDRESS)
                .withEmail("xxx@yyy.com")
                .withUsername("someUser")
                .withPassword("somePassword");
    }

    protected DockerBuildInfo defaultExampleBuildInfo() {
        return new DockerBuildInfo()
                .withDockerfile("**/Dockerfile")
                .withDockerImage("someImage")
                .withDockerImageTag("someTag")
                .withLinuxFxVersion("DOCKER|foo/bar:tag")
                .withAuthConfig(defaultExampleAuthConfig());
    }
}
