/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice;

import com.microsoft.azure.management.appservice.*;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.util.HashMap;

import org.jenkinsci.plugins.microsoft.appservice.commands.*;

public class AppServiceDeploymentCommandContext extends AbstractCommandContext
        implements UploadWarCommand.IUploadWarCommandData,
        GitDeployCommand.IGitDeployCommandData {

    private final String filePath;

    private PublishingProfile pubProfile;

    public AppServiceDeploymentCommandContext(final String filePath) {
        this.filePath = filePath;
    }

    public void configure(AbstractBuild<?, ?> build, BuildListener listener, WebApp app) {
        pubProfile = app.getPublishingProfile();

        HashMap<Class, TransitionInfo> commands = new HashMap<>();

        Class deployCommandClass = null;
        if (app.javaVersion() != JavaVersion.OFF) {
            // For Java application, use FTP-based deployment as it's the recommend way
            deployCommandClass = UploadWarCommand.class;
            commands.put(UploadWarCommand.class, new TransitionInfo(new UploadWarCommand(), null, null));
        } else {
            // For non-Java application, use Git-based deployment
            deployCommandClass = GitDeployCommand.class;
            commands.put(GitDeployCommand.class, new TransitionInfo(new GitDeployCommand(), null, null));
        }

        super.configure(build, listener, commands, deployCommandClass);
        this.setDeploymentState(DeploymentState.Running);
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }
}
