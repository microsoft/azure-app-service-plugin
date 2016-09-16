/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class UploadWarCommand implements ICommand<UploadWarCommand.IUploadWarCommandData> {
    public void execute(UploadWarCommand.IUploadWarCommandData context) {    
		FTPClient ftpClient = new FTPClient();
        try {
        	String publishUrl = context.getPublishUrl(); 
        	String userName = context.getUserName();
        	String passWord = context.getPassWord();
        	String filePath = context.getFilePath();
    		context.logStatus(
            		String.format("Starting deployment of WAR File: %s", filePath));
        	String siteDir = "/site/wwwroot/webapps";
        	if(publishUrl.startsWith("ftp://")) {
        		publishUrl = publishUrl.substring("ftp://".length());
        	}
        	
        	if(publishUrl.indexOf("/") > 0) {
        		int splitIndex = publishUrl.indexOf("/");
        		siteDir = publishUrl.substring(splitIndex);
        		publishUrl = publishUrl.substring(0, splitIndex);
        	}
        	siteDir = "/site/wwwroot/webapps";
        	int lastNameIndex = filePath.lastIndexOf(File.separator);
        	String fileName = filePath.substring(lastNameIndex + 1);
			ftpClient.connect(publishUrl);
			ftpClient.login(userName, passWord);
	        ftpClient.changeWorkingDirectory(siteDir);
	        context.logStatus(
                    String.format("Working directory for FTP upload: %s", ftpClient.printWorkingDirectory()));
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	        try(InputStream stream = new FileInputStream(filePath)){
		        ftpClient.storeFile(fileName, stream);	        	
	        }
	        
    		context.logStatus(
            		String.format("Completed deployment of WAR File: %s", filePath));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}   
    }    
    
    public interface IUploadWarCommandData extends IBaseCommandData {
    	public String getPublishUrl(); 
    	public String getUserName();
    	public String getPassWord();
    	public String getFilePath();
    }
}
