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

import com.microsoft.azure.management.appservice.PublishingProfile;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class UploadWarCommand implements ICommand<UploadWarCommand.IUploadWarCommandData> {

    private static final String ROOT_WAR = "ROOT.war";

    private static final String ROOT_DIR = "ROOT";

    public void execute(UploadWarCommand.IUploadWarCommandData context) {
        final PublishingProfile pubProfile = context.getPublishingProfile();
        FTPClient ftpClient = new FTPClient();
        try {
            String ftpUrl = pubProfile.ftpUrl();
            String userName = pubProfile.ftpUsername();
            String password = pubProfile.ftpPassword();
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

            // Deploying to website root, remove the default directory first
            if (fileName.equals(ROOT_WAR)) {
                removeFtpDirectory(context, ftpClient, ROOT_DIR);
            }

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

    /**
     * Remove FTP directory recursively
     * @param context Command context
     * @param ftpClient FTP client
     * @param dir Directory to remove
     * @throws IOException
     */
    private void removeFtpDirectory(UploadWarCommand.IUploadWarCommandData context, FTPClient ftpClient, String dir) throws IOException {
        context.logStatus("Removing remote directory: " + dir);

        FTPFile[] subFiles = ftpClient.listFiles(dir);
        if (subFiles.length > 0) {
            for (FTPFile ftpFile : subFiles) {
                String fileName = ftpFile.getName();
                if (fileName.equals(".") || fileName.equals("..")) {
                    // Skip
                    continue;
                }

                String fullFileName = dir + "/" + fileName;
                if (ftpFile.isDirectory()) {
                    // Remove sub directory recursively
                    removeFtpDirectory(context, ftpClient, fullFileName);
                } else {
                    // Delete regular file
                    context.logStatus("Removing remote file: " + fullFileName);

                    ftpClient.deleteFile(fullFileName);
                }
            }
        }

        ftpClient.removeDirectory(dir);
    }

    public interface IUploadWarCommandData extends IBaseCommandData {

        public PublishingProfile getPublishingProfile();

        public String getFilePath();
    }
}
