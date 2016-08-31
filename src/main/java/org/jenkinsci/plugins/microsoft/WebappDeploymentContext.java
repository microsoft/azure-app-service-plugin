/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft;

import java.util.Hashtable;

import org.jenkinsci.plugins.microsoft.commands.ICommand;
import org.jenkinsci.plugins.microsoft.commands.ResourceGroupCommand;
import org.jenkinsci.plugins.microsoft.commands.TemplateDeployCommand;
import org.jenkinsci.plugins.microsoft.commands.TemplateMonitorCommand;
import org.jenkinsci.plugins.microsoft.commands.TransitionInfo;
import org.jenkinsci.plugins.microsoft.commands.UploadWarCommand;
import org.jenkinsci.plugins.microsoft.commands.ValidateWebappCommand;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;
import org.jenkinsci.plugins.microsoft.services.IARMTemplateServiceData;
import org.jenkinsci.plugins.microsoft.services.IAzureConnectionData;
import org.jenkinsci.plugins.microsoft.services.ServiceDelegateHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.commands.GetPublishSettingsCommand;
import org.jenkinsci.plugins.microsoft.commands.IBaseCommandData;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.website.WebSiteManagementClient;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class WebappDeploymentContext extends AbstractBaseContext
	implements ResourceGroupCommand.IResourceGroupCommandData, 
		UploadWarCommand.IUploadWarCommandData, 
		ValidateWebappCommand.IValidateWebappCommandData,
		GetPublishSettingsCommand.IGetPublishSettingsCommandData,
		TemplateDeployCommand.ITemplateDeployCommandData, 
		TemplateMonitorCommand.ITemplateMonitorCommandData, 
		IARMTemplateServiceData, 
		Describable<WebappDeploymentContext> {
	
	private IAzureConnectionData connectData;
	private ResourceManagementClient resourceClient;
	private WebSiteManagementClient websiteClient;

	private String deploymentName;
	private String publishUrl; 
	private String userName;
	private String passWord; 
	private String resourceGroupName;
	private String hostingPlanName;
	private String webappName;
	private String skuName;
	private String skuCapacity;
	private String filePath;
    private String location;

    private static final String EMBEDDED_TEMPLATE_FILENAME = "/templateValue.json";
    
    @DataBoundConstructor
	public WebappDeploymentContext(
            final String resourceGroupName,
            final String hostingPlanName,
            final String webappName,
            final String skuName,
            final String skuCapacity,
            final String filePath,
            final String location) {
	    this.resourceGroupName = resourceGroupName;
	    this.hostingPlanName = hostingPlanName;
	    this.webappName = webappName;
	    this.skuName = skuName;
	    this.skuCapacity = skuCapacity;
	    this.filePath = filePath;
	    this.location = location;
  }
	
    @SuppressWarnings("unchecked")
	@Override
    public Descriptor<WebappDeploymentContext>  getDescriptor() {
    	return Jenkins.getInstance().getDescriptor(getClass());
    }

	public String getResourceGroupName() {
		return this.resourceGroupName;
	}

	public String getHostingPlanName() {
		return this.hostingPlanName;
	}

	@Override
	public String getWebappName() {
		return this.webappName;
	}

	public String getSkuName() {
		return this.skuName;
	}

	public String getSkuCapacity() {
		return this.skuCapacity;
	}

	@Override
	public String getFilePath() {
		return this.filePath;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	public void setPublishUrl(String publishUrl) {
		this.publishUrl = publishUrl;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public void setPassWord(String passWord) { 
		this.passWord = passWord;
	}
 
	public String getDeploymentName() {
		return this.deploymentName;
	}
	
	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}
	
	public String getPublishUrl() {
		return this.publishUrl;
	}
	
	public String getUserName() {
		return this.userName;
	}
	
	public String getPassWord() {
		return this.passWord;
	}
	
	@Override
	public IBaseCommandData getDataForCommand(ICommand command) {
		return this;
	}

	public ResourceManagementClient getResourceClient() {
		return this.resourceClient;
	}
		
	public WebSiteManagementClient getWebsiteClient() {
		return this.websiteClient;
	}
		
	public void configure(BuildListener listener, IAzureConnectionData connectData) throws AzureCloudException {
		this.connectData = connectData;
		resourceClient = ServiceDelegateHelper.getResourceManagementClient(ServiceDelegateHelper.load(connectData));
		websiteClient = ServiceDelegateHelper.getWebsiteManagementClient(ServiceDelegateHelper.load(connectData));
    	
		Hashtable<Class, TransitionInfo> commands = new Hashtable<Class, TransitionInfo>();
		commands.put(ResourceGroupCommand.class, new TransitionInfo(new ResourceGroupCommand(), ValidateWebappCommand.class, null));
		commands.put(ValidateWebappCommand.class, new TransitionInfo(new ValidateWebappCommand(), GetPublishSettingsCommand.class, TemplateDeployCommand.class));
		commands.put(GetPublishSettingsCommand.class, new TransitionInfo(new GetPublishSettingsCommand(), UploadWarCommand.class, null));
		commands.put(TemplateDeployCommand.class, new TransitionInfo(new TemplateDeployCommand(), TemplateMonitorCommand.class, null));
		commands.put(TemplateMonitorCommand.class, new TransitionInfo(new TemplateMonitorCommand(), GetPublishSettingsCommand.class, null));
		commands.put(UploadWarCommand.class, new TransitionInfo(new UploadWarCommand(), null, null));
		super.configure(listener, commands, ResourceGroupCommand.class);
		this.setDeploymentState(DeploymentState.Running);
	}

	@Override
	public String getEmbeddedTemplateName() {
		return EMBEDDED_TEMPLATE_FILENAME;
	}

	@Override
	public void configureTemplate(JsonNode tmp) throws IllegalAccessException, AzureCloudException {
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.hostingPlanName, "hostingPlanName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.webappName, "webSiteName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("string", this.skuName, "skuName", null, tmp);
        AzureManagementServiceDelegate.validateAndAddFieldValue("int", this.skuCapacity, "skuCapacity", null, tmp);
	}

	@Override
	public IARMTemplateServiceData getArmTemplateServiceData() {
		return this;
	}

	@Override
	public IAzureConnectionData getAzureConnectionData() {
		return this.connectData;
	}
	
    @Extension
    public static final class DescriptorImpl extends WebappDeploymentContextDescriptor {
    }
}
