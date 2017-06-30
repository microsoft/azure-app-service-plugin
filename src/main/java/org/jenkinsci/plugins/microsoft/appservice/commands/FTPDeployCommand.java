/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.microsoft.azure.management.appservice.PublishingProfile;
import hudson.FilePath;
import hudson.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;

public class FTPDeployCommand implements ICommand<FTPDeployCommand.IFTPDeployCommandData> {

    private static final String SITE_ROOT = "/site/wwwroot/";

    // Java specific
    private static final String TOMCAT_ROOT_WAR = SITE_ROOT + "webapps/ROOT.war";
    private static final String TOMCAT_ROOT_DIR = SITE_ROOT + "webapps/ROOT";

    private static final class FTPException extends Exception {

        FTPException(String msg) {
            super(msg);
        }

    }

    public void execute(IFTPDeployCommandData context) {
        final FilePath workspace = context.getWorkspace();
        final PublishingProfile pubProfile = context.getPublishingProfile();

        if (workspace == null) {
            context.logError("Workspace is null");
            context.setDeploymentState(DeploymentState.HasError);
            return;
        }

        FTPClient ftpClient = new FTPClient();
        try {
            String ftpUrl = pubProfile.ftpUrl();
            String userName = pubProfile.ftpUsername();
            String password = pubProfile.ftpPassword();

            if (ftpUrl.startsWith("ftp://")) {
                ftpUrl = ftpUrl.substring("ftp://".length());
            }

            if (ftpUrl.indexOf("/") > 0) {
                int splitIndex = ftpUrl.indexOf("/");
                ftpUrl = ftpUrl.substring(0, splitIndex);
            }

            context.logStatus(String.format("Starting to deploy to FTP: %s", ftpUrl));

            ftpClient.connect(ftpUrl);
            if (!ftpClient.login(userName, password)) {
                throw new FTPException("Fail to login");
            }

            // Use passive mode to bypass client firewall
            ftpClient.enterLocalPassiveMode();

            final String targetDirectory = SITE_ROOT + Util.fixNull(context.getTargetDirectory());
            if (!ftpClient.changeWorkingDirectory(targetDirectory)) {
                // Target directory doesn't exist. Try to create it.
                if (!ftpClient.makeDirectory(targetDirectory)) {
                    throw new FTPException("Fail to make directory: " + targetDirectory);
                }
            }

            if (!ftpClient.changeWorkingDirectory(targetDirectory)) {
                throw new FTPException("Fail to change working directory to: " + targetDirectory);
            }

            context.logStatus(String.format("Working directory: %s", ftpClient.printWorkingDirectory()));

            final FilePath sourceDir = workspace.child(Util.fixNull(context.getSourceDirectory()));
            final FilePath[] files = sourceDir.list(context.getFilePath());
            for (final FilePath file : files) {
                uploadFile(context, ftpClient, sourceDir, file);
            }
        } catch (FTPException | IOException | InterruptedException e) {
            context.logError("Fail to deploy to FTP: " + e.getMessage());
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
     * Remove FTP directory recursively.
     * @param context Command context
     * @param ftpClient FTP client
     * @param dir Directory to remove
     * @throws IOException
     */
    private void removeFtpDirectory(IFTPDeployCommandData context, FTPClient ftpClient, String dir)
            throws IOException, FTPException {
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

                    if (!ftpClient.deleteFile(fullFileName)) {
                        throw new FTPException("Fail to delete file: " + fullFileName);
                    }
                }
            }
        }

        if (!ftpClient.removeDirectory(dir)) {
            throw new FTPException("Fail to remove directory: " + dir);
        }
    }

    private void uploadFile(IFTPDeployCommandData context, FTPClient ftpClient, FilePath sourceDir, FilePath file)
            throws IOException, FTPException, InterruptedException {

        final String remoteName = getRemoteFileName(sourceDir, file);
        context.logStatus(String.format("Uploading %s", remoteName));

        // Need some preparation in some cases
        prepareDirectory(context, ftpClient, remoteName);

        if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
            throw new FTPException("Fail to set FTP file type to binary");
        }

        try (InputStream stream = file.read()) {
            if (!ftpClient.storeFile(remoteName, stream)) {
                throw new FTPException("Fail to upload file to: " + remoteName);
            }
        }
    }

    private String getRemoteFileName(FilePath sourceDir, FilePath file) {
        final String prefix = sourceDir.getRemote();
        final String filePath = file.getRemote();
        if (filePath.startsWith(prefix)) {
            return FilenameUtils.separatorsToUnix(filePath.substring(prefix.length() + 1));
        } else {
            return FilenameUtils.separatorsToUnix(filePath);
        }
    }


    private void prepareDirectory(IFTPDeployCommandData context, FTPClient ftpClient, String fileName)
            throws IOException, FTPException {
        // Deployment to tomcat root requires removing root directory first
        final String targetFilePath = FilenameUtils.concat(ftpClient.printWorkingDirectory(), fileName);
        if (targetFilePath.equalsIgnoreCase(FilenameUtils.separatorsToSystem(TOMCAT_ROOT_WAR))) {
            removeFtpDirectory(context, ftpClient, TOMCAT_ROOT_DIR);
        }
    }

    public interface IFTPDeployCommandData extends IBaseCommandData {

        PublishingProfile getPublishingProfile();

        String getFilePath();

        String getSourceDirectory();

        String getTargetDirectory();
    }
}
