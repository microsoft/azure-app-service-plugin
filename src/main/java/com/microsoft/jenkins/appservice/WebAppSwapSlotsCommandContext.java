package com.microsoft.jenkins.appservice;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.jenkins.appservice.commands.SwapSlotsCommand;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public class WebAppSwapSlotsCommandContext extends BaseCommandContext
        implements SwapSlotsCommand.ISwapSlotsCommandData {
    private WebApp webApp;
    private String sourceSlotName;
    private String targetSlotName;


    public void configure(
            final Run<?, ?> run,
            final FilePath workspace,
            final Launcher launcher,
            final TaskListener listener,
            final WebApp app) throws AzureCloudException {
        this.webApp = app;
        CommandService.Builder builder = CommandService.builder();
        builder.withStartCommand(SwapSlotsCommand.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        super.configure(jobContext, builder.build());
    }

    public void setWebApp(WebApp webApp) {
        this.webApp = webApp;
    }

    public void setSourceSlotName(String sourceSlotName) {
        this.sourceSlotName = sourceSlotName;
    }

    public void setTargetSlotName(String targetSlotName) {
        this.targetSlotName = targetSlotName;
    }

    @Override
    public String getSourceSlotName() {
        return this.sourceSlotName;
    }

    @Override
    public String getTargetSlotName() {
        return this.targetSlotName;
    }

    @Override
    public WebApp getWebApp() {
        return this.webApp;
    }

    @Override
    public StepExecution startImpl(StepContext context) throws Exception {
        return null;
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }
}
