/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;

import com.microsoft.azure.management.resources.ResourceManagementClient;

public class TemplateMonitorCommand implements ICommand<TemplateMonitorCommand.ITemplateMonitorCommandData> {
	public void execute(TemplateMonitorCommand.ITemplateMonitorCommandData context) {
		String deploymentName = context.getDeploymentName();
		String rcName  = context.getResourceGroupName(); 
        ResourceManagementClient rmc = context.getResourceClient();
    	boolean deploySuccess = AzureManagementServiceDelegate.monitor(rmc, rcName, deploymentName, context);
        if(deploySuccess) {
        	context.setDeploymentState(DeploymentState.Success);
	        context.logStatus(
	        		String.format("Azure '%s' deployed successfully.", deploymentName));
        }else {
	        context.logError(
	        		String.format("Azure '%s' depoyment unsuccessfully.", deploymentName));
        }
	}
	
	public interface ITemplateMonitorCommandData extends IBaseCommandData {
		public String getDeploymentName();
		public String getResourceGroupName();
		public ResourceManagementClient getResourceClient();
	}
}
