/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class UploadWarCommand implements ICommand<UploadWarCommand.IUploadWarCommandData> {

    public void execute(UploadWarCommand.IUploadWarCommandData context) {
        FTPClient ftpClient = new FTPClient();
        try {
            String ftpUrl = context.getFTPUrl();
            String userName = context.getFTPUserName();
            String password = context.getFTPPassword();
            String filePath = context.getFilePath();
            context.logStatus(
                    String.format("Starting deployment of WAR File: %s", filePath));

            if (ftpUrl.startsWith("ftp://")) {
                ftpUrl = ftpUrl.substring("ftp://".length());
            }

            if (ftpUrl.indexOf("/") > 0) {
                int splitIndex = ftpUrl.indexOf("/");
                ftpUrl = ftpUrl.substring(0, splitIndex);
            }

            final String siteDir = "/site/wwwroot/webapps";
            int lastNameIndex = filePath.lastIndexOf(File.separator);
            String fileName = filePath.substring(lastNameIndex + 1);
            ftpClient.connect(ftpUrl);
            ftpClient.login(userName, password);
            ftpClient.makeDirectory(siteDir);
            ftpClient.changeWorkingDirectory(siteDir);
            context.logStatus(
                    String.format("Working directory for FTP upload: %s", ftpClient.printWorkingDirectory()));

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            try (InputStream stream = new FileInputStream(filePath)) {
                ftpClient.storeFile(fileName, stream);
            }

            context.logStatus(
                    String.format("Completed deployment of WAR File: %s", filePath));
        } catch (IOException e) {
            e.printStackTrace();
            context.logError("Fail to deploy WAR file: "  + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    context.logStatus("Fail to disconnect from FTP: " + e.getMessage());
                }
            }
        }
    }

    public interface IUploadWarCommandData extends IBaseCommandData {

        public String getFTPUrl();

        public String getFTPUserName();

        public String getFTPPassword();

        public String getFilePath();
    }
}
