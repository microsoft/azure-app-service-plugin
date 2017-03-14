# Azure App Service Plugin


Jenkins Plugin to create an Azure App Service (currently supports only Azure Java WebApps).

## Pre-requirements
Register and authorize your client application.

Retrieve and use Client ID and Client Secret to be sent to Azure AD during authentication.

Refer to
  * [Adding, Updating, and Removing an Application](https://msdn.microsoft.com/en-us/library/azure/dn132599.aspx) 
  * [Register a client app](https://msdn.microsoft.com/en-us/dn877542.asp)

## How to install the Azure App Service Plugin
1. Download the azure-app-service.hpi file from [here](https://github.com/Microsoft/azure-webapp-plugin/blob/master/install/azure-webapp-plugin.hpi)
1. Within the Jenkins dashboard, click Manage Jenkins.
2. In the Manage Jenkins page, click Manage Plugins.
3. Click the Advanced tab.
4. Click on the Choose file button in the Upload Plugin section and choose the azure-webapp-plugin.hpi file.
5. Click the Upload button in the Upload Plugin section.
6. Click either “Install without restart” or “Download now and install after restart”.
7. Restart Jenkins if necessary.

## Configure the plugin
1. Within the Jenkins dashboard, Select a Job then select Configure
2. Scroll to the "Add post-build action" drop down.  
3. Select "Azure WebApp Configuration" 
4. Enter the subscription ID, Client ID, Client Secret and the OAuth 2.0 Token Endpoint in the Azure Profile Configuration section.
5. Enter the Resource Group Name, Location, Hosting Plan Name, Web App Name, Sku Name, Sku Capacity, War File Path in the WebApp Configuration section.
7. Save Job and click on Build now.
8. Jenkins will create an Azure WebApp and deploy the War file to the WebApp if it doesn't exist.  Otherwise, the War file will be deployed to the existing WebApp. 
9. Logs are available in the builds console logs.


 
