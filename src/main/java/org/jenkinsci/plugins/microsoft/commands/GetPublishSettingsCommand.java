/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

import com.microsoft.azure.util.AzureCredentials;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GetPublishSettingsCommand implements ICommand<GetPublishSettingsCommand.IGetPublishSettingsCommandData> {

    @Override
    public void execute(GetPublishSettingsCommand.IGetPublishSettingsCommandData context) {
        /*try {
            context.logStatus("Retrieving FTP publish settings.");
            String resourceGroupName = context.getResourceGroupName();
            String name = context.getWebappName();

            WebSiteManagementClient website = context.getWebsiteClient();
            StringBuffer buffer = new StringBuffer();
            try (BufferedReader inStream = new BufferedReader(new InputStreamReader(website.getSitesOperations().listSitePublishingProfileXml(resourceGroupName, name, "Ftp").getBody()))) {
                String line = inStream.readLine();
                while (line != null) {
                    buffer.append(line);
                    line = inStream.readLine();
                }
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(new java.io.ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8)));
            NodeList nodeList = doc.getElementsByTagName("publishProfile");
            for (int i = 0; i < nodeList.getLength(); i++) {
                NamedNodeMap attributes = nodeList.item(i).getAttributes();
                Node node = attributes.getNamedItem("publishMethod");
                if (node != null && "FTP".equals(node.getNodeValue())) {
                    String appUrl = attributes.getNamedItem("destinationAppUrl").getNodeValue();
                    context.logStatus("Destination Application Url: " + appUrl);
                    context.setPublishUrl(attributes.getNamedItem("publishUrl").getNodeValue());
                    context.setUserName(attributes.getNamedItem("userName").getNodeValue());
                    context.setPassWord(attributes.getNamedItem("userPWD").getNodeValue());
                    context.setDeploymentState(DeploymentState.Success);
                    context.logStatus("Successfully retrieved FTP publish settings");
                    return;
                }
            }

            context.logError("FTP profile information not found");
            context.setDeploymentState(DeploymentState.HasError);
        } catch (CloudException | IllegalArgumentException | IOException
                | ParserConfigurationException | SAXException e) {
            context.logError("Error retrieving FTP publish settings: " + e.getMessage());
            e.printStackTrace();
        }*/
    }

    public interface IGetPublishSettingsCommandData extends IBaseCommandData {

        public String getResourceGroupName();

        public String getWebappName();

        /*public void setPublishUrl(String publishUrl);

        public void setUserName(String userName);

        public void setPassWord(String passWord);*/

        public AzureCredentials.ServicePrincipal getAzureServicePrincipal();
    }
}
