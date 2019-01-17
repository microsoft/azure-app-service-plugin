# Azure App Service Plugin

A Jenkins plugin to deploy an Azure App Service (currently supports only Web App).

## How to Install

You can install/update this plugin in Jenkins update center (Manage Jenkins -> Manage Plugins, search Azure App Service Plugin).

You can also manually install the plugin if you want to try the latest feature before it's officially released.
To manually install the plugin:

1. Clone the repo and build:
   ```
   mvn package
   ```
2. Open your Jenkins dashboard, go to Manage Jenkins -> Manage Plugins.
3. Go to Advanced tab, under Upload Plugin section, click Choose File.
4. Select `azure-app-service.hpi` in `target` folder of your repo, click Upload.
5. Restart your Jenkins instance after install is completed.

## Deploy to Azure Web App

### Prerequisites

To use this plugin to deploy to Azure Web App, first you need to have an Azure Service Principal in your Jenkins instance.

1. Create an Azure Service Principal through [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?toc=%2fazure%2fazure-resource-manager%2ftoc.json) or [Azure portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal).
2. Open Jenkins dashboard, go to Credentials, add a new Microsoft Azure Service Principal with the credential information you just created.

Then create a Web App in Azure portal or through Azure CLI, we support both [Web App](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/Microsoft.WebSite?tab=Overview) (which is hosted on Windows) and [Web App On Linux](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/Microsoft.AppSvcLinux?tab=Overview).

### Deploy to Web App through File Upload

You can deploy your project to Azure Web App by uploading your build artifacts (for example, `.war` or `.zip` file in Java). 
- For Java EE apps, [WAR deploy](https://docs.microsoft.com/en-us/azure/app-service/app-service-deploy-zip#deploy-war-file) will be used. 
- For Java SE app, [ZIP deploy](https://docs.microsoft.com/en-us/azure/app-service/app-service-deploy-zip#deploy-zip-file) will be used.
- For other languages, Git will be used.

1. Create a new freestyle project in Jenkins, add necessary build steps to build your code.
2. Add a post-build action 'Publish an Azure Web App'.
3. Select your Azure credential in Azure Profile Configuration section.
4. In App Configuration section, choose the resource group and web app in your subscription, and also fill in the files you want to deploy (for example, a war package if you're using Java).
5. There are two optional parameters Source Directory and Target Directory that allows you to specify source and target folders when uploading files. For example, Java web app on Azure is actually running in a Tomcat server. So you should upload you war package to `webapps` folder. So in this case, set Target Directory to `webapps`.
6. You can also set Slot Name if you want to deploy to a slot other than production.
7. Save the project and build it, your web app will be deployed to Azure when build is completed.

> For Java SE apps, you need to archive a web.config file with your jar file in zip format. The basic web.config file example is as below, and you need to replace the variable $JAR_FILE with your own jar file name.
> ```xml
> <?xml version="1.0" encoding="UTF-8"?>
><configuration>
>  <system.webServer>
>    <handlers>
>      <add name="httpPlatformHandler" path="*" verb="*" modules="httpPlatformHandler" resourceType="Unspecified" />
>    </handlers>
>    <httpPlatform processPath="%JAVA_HOME%\bin\java.exe"
>        arguments="-Djava.net.preferIPv4Stack=true -Dserver.port=%HTTP_PLATFORM_PORT% -jar &quot;%HOME%\site\wwwroot\${JAR_FILE}&quot;">
>    </httpPlatform>
>  </system.webServer>
></configuration>
> ```

### Deploy to Web App on Linux using Docker

Web App on Linux supports a different way to do the deployment using Docker. To deploy using Docker, you need to provide a `Dockerfile` that packages your web app with service runtime into a docker image. Then the plugin will build the image, push it to a docker registry and deploy the image to your web app.

> Web App on Linux also supports traditional ways like Git and WAR deploy, but only for built-in languages (.NET Core, Java, Node.js, PHP and Ruby). For other languages, you need to package your application code and service runtime together into a docker image and use docker to deploy.

To deploy using docker:

1. Same as #1 - #3 of file upload approach.
2. Select a Linux web app, choose Publish via Docker.
3. Fill in Dockerfile path (by default it's `**/Dockerfile`), Docker registry URL (leave it empty if you use DockerHub), Registry credentials.
4. Optionally you can specify the docker image name and tag in Advanced tab, by default image name is get from the image name you configured in Azure portal (in Docker Container setting) and tag is generated from `$BUILD_NUMBER`. So please specify the image name in at least one place (in Docker Container settings in portal or Docker Image in advanced options).
5. Please also be noted deployment will fail if you're used built-in docker image setting. So please first go to Docker Container setting in Azure portal to change docker config to use custom image. For built-in image, please use file upload approach to deploy.
6. Similar to file upload approach, you can choose a different slot other than production.
7. Save and build the project, you'll see your container image is pushed to your registry and web app is deployed.

## Deploy using Pipeline

You can also use this plugin in pipeline (Jenkinsfile). Here are some samples to use the plugin in pipeline script:

To deploy a Java web app using file upload:

```groovy
azureWebAppPublish azureCredentialsId: '<credential_id>',
                   resourceGroup: '<resource_group_name>', appName: '<app_name>',
                   filePath: '*.war', sourceDirectory: 'target', targetDirectory: 'webapps'
```

To deploy using docker:

```groovy
azureWebAppPublish azureCredentialsId: '<credential_id>', publishType: 'docker',
                   resourceGroup: '<resource_group_name>', appName: '<app_name>',
                   dockerImageName: '<image_name>', dockerImageTag: '<image_tag>',
                   dockerRegistryEndpoint: [credentialsId: '<registry_credential_id>', url: "<registry_url>"]
```

For advanced options, you can use Jenkins Pipeline Syntax tool to generate a sample script.

## Data/Telemetry

Azure App Service Plugin collects usage data and sends it to Microsoft to help improve our products and services. Read our [privacy statement](http://go.microsoft.com/fwlink/?LinkId=521839) to learn more.

You can turn off usage data collection in Manage Jenkins -> Configure System -> Azure -> Help make Azure Jenkins plugins better by sending anonymous usage statistics to Azure Application Insights.

## Reporting bugs and feature requests

We use [Jenkins JIRA](https://issues.jenkins-ci.org/) to record all bugs and feature requests. Please follow below steps to create your own issues.

1. Search in Jira to see if the issue was existed already.
2. Create a new issue with the component `azure-app-service-plugin` .

You can refer to [Jira doc](https://confluence.atlassian.com/jiracoreserver/creating-issues-and-sub-tasks-939937904.html#Creatingissuesandsub-tasks-Creatinganissue) for detailed instructions about creating an issue.