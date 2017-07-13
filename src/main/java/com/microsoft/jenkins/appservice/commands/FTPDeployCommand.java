/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.jenkins.appservice.util.FilePathUtils;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;
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

        FTPException(final String msg) {
            super(msg);
        }

        FTPException(final Exception ex) {
            super(ex);
        }

    }

    public void execute(final IFTPDeployCommandData context) {
        final FilePath workspace = context.getWorkspace();
        final PublishingProfile pubProfile = context.getPublishingProfile();

        if (workspace == null) {
            context.logError("Workspace is null");
            context.setDeploymentState(DeploymentState.HasError);
            return;
        }

        String ftpUrl = pubProfile.ftpUrl();
        if (ftpUrl.startsWith("ftp://")) {
            ftpUrl = ftpUrl.substring("ftp://".length());
        }

        if (ftpUrl.indexOf("/") > 0) {
            int splitIndex = ftpUrl.indexOf("/");
            ftpUrl = ftpUrl.substring(0, splitIndex);
        }

        try {
            workspace.act(new FTPDeployCommandOnSlave(
                context.getListener(),
                ftpUrl,
                pubProfile.ftpUsername(),
                pubProfile.ftpPassword(),
                workspace,
                context.getSourceDirectory(),
                context.getTargetDirectory(),
                context.getFilePath()
            ));
        } catch (IOException | FTPException e) {
            context.logError("Fail to deploy to FTP: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class FTPDeployCommandOnSlave extends MasterToSlaveCallable<Void, FTPException> {

        private final TaskListener listener;
        private final String ftpUrl;
        private final String ftpUserName;
        private final String ftpPassword;
        private final FilePath workspace;
        private final String sourceDirectory;
        private final String targetDirectory;
        private final String filePath;

        private FTPDeployCommandOnSlave(
                final TaskListener listener,
                final String ftpUrl,
                final String ftpUserName,
                final String ftpPassword,
                final FilePath workspace,
                final String sourceDirectory,
                final String targetDirectory,
                final String filePath) {
            this.listener = listener;
            this.ftpUrl = ftpUrl;
            this.ftpUserName = ftpUserName;
            this.ftpPassword = ftpPassword;
            this.workspace = workspace;
            this.sourceDirectory = sourceDirectory;
            this.targetDirectory = targetDirectory;
            this.filePath = filePath;
        }


        @Override
        public Void call() throws FTPException {
            FTPClient ftpClient = new FTPClient();
            try {
                listener.getLogger().println(String.format("Starting to deploy to FTP: %s", ftpUrl));

                ftpClient.connect(ftpUrl);
                if (!ftpClient.login(ftpUserName, ftpPassword)) {
                    throw new FTPException("Fail to login");
                }

                // Use passive mode to bypass client firewall
                ftpClient.enterLocalPassiveMode();

                final String absTargetDirectory = SITE_ROOT + Util.fixNull(targetDirectory);
                if (!ftpClient.changeWorkingDirectory(absTargetDirectory)) {
                    // Target directory doesn't exist. Try to create it.
                    if (!ftpClient.makeDirectory(absTargetDirectory)) {
                        throw new FTPException("Fail to make directory: " + absTargetDirectory);
                    }
                }

                if (!ftpClient.changeWorkingDirectory(absTargetDirectory)) {
                    throw new FTPException("Fail to change working directory to: " + absTargetDirectory);
                }

                listener.getLogger().println(String.format("Working directory: %s", ftpClient.printWorkingDirectory()));

                final FilePath sourceDir = workspace.child(Util.fixNull(sourceDirectory));
                final FilePath[] files = sourceDir.list(filePath);

                if (files.length == 0) {
                    listener.getLogger().println("No file found. Skip deployment.");
                    return null;
                }

                for (final FilePath file : files) {
                    uploadFile(ftpClient, sourceDir, file);
                }
            } catch (IOException | InterruptedException e) {
                throw new FTPException(e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                        listener.getLogger().println("Fail to disconnect from FTP: " + e.getMessage());
                    }
                }
            }

            return null;
        }

        /**
         * Remove FTP directory recursively.
         * @param ftpClient FTP client
         * @param dir Directory to remove
         * @throws IOException
         */
        private void removeFtpDirectory(final FTPClient ftpClient, final String dir)
                throws IOException, FTPException {
            String cwd = ftpClient.printWorkingDirectory();
            // Return if folder does not exist
            if (!ftpClient.changeWorkingDirectory(dir)) {
                return;
            }

            ftpClient.changeWorkingDirectory(cwd);

            listener.getLogger().println("Removing remote directory: " + dir);

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
                        removeFtpDirectory(ftpClient, fullFileName);
                    } else {
                        // Delete regular file
                        listener.getLogger().println("Removing remote file: " + fullFileName);

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

        private void uploadFile(final FTPClient ftpClient, final FilePath sourceDir, final FilePath file)
                throws IOException, FTPException, InterruptedException {

            final String remoteName = FilenameUtils.separatorsToUnix(
                    FilePathUtils.trimDirectoryPrefix(sourceDir, file));
            listener.getLogger().println(String.format("Uploading %s", remoteName));

            // Need some preparation in some cases
            prepareDirectory(ftpClient, remoteName);

            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
                throw new FTPException("Fail to set FTP file type to binary");
            }

            try (InputStream stream = file.read()) {
                if (!ftpClient.storeFile(remoteName, stream)) {
                    throw new FTPException("Fail to upload file to: " + remoteName);
                }
            }
        }

        private void prepareDirectory(final FTPClient ftpClient, final String fileName)
                throws IOException, FTPException {
            // Deployment to tomcat root requires removing root directory first
            final String targetFilePath = FilenameUtils.concat(ftpClient.printWorkingDirectory(), fileName);
            if (targetFilePath.equalsIgnoreCase(FilenameUtils.separatorsToSystem(TOMCAT_ROOT_WAR))) {
                removeFtpDirectory(ftpClient, TOMCAT_ROOT_DIR);
            }
        }
    }

    public interface IFTPDeployCommandData extends IBaseCommandData {

        PublishingProfile getPublishingProfile();

        String getFilePath();

        String getSourceDirectory();

        String getTargetDirectory();
    }
}
