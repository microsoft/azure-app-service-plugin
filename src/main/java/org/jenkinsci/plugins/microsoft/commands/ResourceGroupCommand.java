package org.jenkinsci.plugins.microsoft.commands;

import java.io.IOException;
import java.net.URISyntaxException;

import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroup;
import com.microsoft.azure.management.resources.models.ResourceGroupCreateOrUpdateResult;
import com.microsoft.windowsazure.exception.ServiceException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ResourceGroupCommand implements ICommand<ResourceGroupCommand.IResourceGroupCommandData> {
	public void execute(ResourceGroupCommand.IResourceGroupCommandData context) {
		try {
			String resourceGroupName = context.getResourceGroupName();
			String location = context.getLocation();
	        context.logStatus(String.format("Creating resource group '%s' if it does not exist", resourceGroupName));
	        ResourceManagementClient rmc = context.getResourceClient();
	        ResourceGroup parameters = new ResourceGroup();
	        parameters.setLocation(location);
			ResourceGroupCreateOrUpdateResult response = rmc.getResourceGroupsOperations().createOrUpdate(resourceGroupName, parameters);
			if(response.getStatusCode() < 200 || response.getStatusCode() > 299) {
	        	context.logError("Error creating resource group.");
	        	return;
	        }

			context.setDeploymentState(DeploymentState.Success);
		} catch (IOException | IllegalArgumentException | ServiceException | URISyntaxException e) {
			context.logError("Error creating resource group:", e);
		}
	}
	
	public interface IResourceGroupCommandData extends IBaseCommandData {
		public String getResourceGroupName();
		public String getLocation();
		public ResourceManagementClient getResourceClient();
	}
}
