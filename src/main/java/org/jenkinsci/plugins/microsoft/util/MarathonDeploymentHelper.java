/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import hudson.FilePath;
import hudson.model.BuildListener;
import jenkins.model.Jenkins;

public class MarathonDeploymentHelper {
    public static void update(String host, String marathonConfigFile, String sshFile, 
    		String filePassword, String linuxAdminUsername, BuildListener listener) 
    		throws IOException, InterruptedException, AzureCloudException {    	
        JSch jsch=new JSch();
        Session session = null;
        try {
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			jsch.addIdentity(sshFile, filePassword);
			session=jsch.getSession(linuxAdminUsername, host, 2200);
			session.setConfig(config);
			session.connect();
			
			ChannelSftp channel = null;
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			String appId = JsonHelper.getId(marathonConfigFile);
			String deployedFilename = "acsDep" + Calendar.getInstance().getTimeInMillis() + ".json";
	        listener.getLogger().println("Copying marathon file to remote file: " + deployedFilename);
			try {
				channel.put(marathonConfigFile, deployedFilename);
			} catch (SftpException e) {
		        listener.getLogger().println("Error creating remote file:" + e.getMessage());
				e.printStackTrace();
			}
			channel.disconnect();
			
			//ignore if app does not exist
	        listener.getLogger().println(String.format("Deleting application with appId: '%s' if it exists", appId));
			MarathonDeploymentHelper.executeCommand(session, "curl -X DELETE localhost:8080/v2/apps/" + appId, listener);
	        listener.getLogger().println(String.format("Deploying file '%s' with appId to marathon.", deployedFilename, appId));
			MarathonDeploymentHelper.executeCommand(session, "curl -i -H 'Content-Type: application/json' -d@" + deployedFilename + " localhost:8080/v2/apps", listener);
		} catch (JSchException e) {
	        listener.error("Error deploying application to marathon:" + e.getMessage());
			e.printStackTrace();
    		throw new AzureCloudException(e.getMessage());
		}finally {
			if(session != null) {session.disconnect();}
		}   
    }
    
    private static void executeCommand(Session session, String command, BuildListener listener) 
    		throws IOException, JSchException, AzureCloudException {
    	ChannelExec execChnl = (ChannelExec)session.openChannel("exec");
		execChnl.setCommand(command); 
		
		try {
			 InputStream in=execChnl.getInputStream();
			 execChnl.connect();
			 try {
		      while(true){
		        if(execChnl.isClosed()){
		        	if(execChnl.getExitStatus() < 0) {
		        		throw new AzureCloudException("Error building or running docker image. Process exected with status: " + 
		        				execChnl.getExitStatus());
		        	}
		          System.out.println("exit-status: "+execChnl.getExitStatus());
		          break;
		        }
		        try{Thread.sleep(1000);}catch(Exception ee){}
		     }
			 }finally {
				 execChnl.disconnect();
			 }
		}catch(AzureCloudException ex) {
			throw ex;
		}
    }
}
