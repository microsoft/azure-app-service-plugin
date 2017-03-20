/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import org.kohsuke.stapler.DataBoundConstructor;

import org.jenkinsci.plugins.microsoft.services.CommandService;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.jenkinsci.plugins.microsoft.commands.AbstractCommandContext;

public class WebAppDeploymentRecorder extends Recorder {

    private final WebAppDeploymentContext context;

    @DataBoundConstructor
    public WebAppDeploymentRecorder(
            final WebAppDeploymentContext context) {
        this.context = context;
    }

    public WebAppDeploymentContext getContext() {
        return this.context;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        // TODO Auto-generated method stub
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        /*
         * This should be run before the build is finalized.
         */
        return false;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Starting Azure Container Service Deployment");
        WebAppDeploymentCommandContext commandContext = this.context.getCommandContext();
        
        commandContext.configure(listener);

        CommandService.executeCommands(commandContext);

        if (commandContext.getHasError()) {
            return false;
        } else {
            listener.getLogger().println("Done Azure Container Service Deployment");
            return true;
        }
    }

    /**
     * Descriptor for ACSDeployRecorderDescriptor. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Azure Webapp Configuration";
        }
    }
}
