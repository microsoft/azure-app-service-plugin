package org.jenkinsci.plugins.microsoft.commands;

import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;
import org.jenkinsci.plugins.microsoft.services.IARMTemplateServiceData;

public class TemplateDeployCommand implements ICommand<TemplateDeployCommand.ITemplateDeployCommandData> {
	public void execute(TemplateDeployCommand.ITemplateDeployCommandData context) {
	    	context.logStatus("Starting deployment");
	        try {
	        	context.setDeploymentName(AzureManagementServiceDelegate.deploy(context.getArmTemplateServiceData()));
				context.setDeploymentState(DeploymentState.Success);
		        context.logStatus("Deployment started.");
			} catch (AzureCloudException e) {
				context.logError("Error starting deployment:", e);
			}
	}
	
	public interface ITemplateDeployCommandData extends IBaseCommandData {
		public IARMTemplateServiceData getArmTemplateServiceData();
		public void setDeploymentName(String deploymentName);
	}
}
