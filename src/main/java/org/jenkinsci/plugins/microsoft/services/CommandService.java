package org.jenkinsci.plugins.microsoft.services;

import java.util.Hashtable;

import org.jenkinsci.plugins.microsoft.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.commands.ICommand;
import org.jenkinsci.plugins.microsoft.commands.TransitionInfo;

public class CommandService {
	public static boolean executeCommands(ICommandServiceData commandServiceData) {
		Class startCommand = commandServiceData.getStartCommandClass(); 
		Hashtable<Class, TransitionInfo> commands = commandServiceData.getCommands();
		if(!commands.isEmpty() && startCommand != null) {
			//successfully started
			TransitionInfo current = commands.get(startCommand);
			while(current != null) {
				ICommand<IBaseCommandData> command = current.getCommand();
				IBaseCommandData commandData = commandServiceData.getDataForCommand(command);
				command.execute(commandData);
				TransitionInfo previous = current;
				current = null;
				
				if(commandData.getDeploymentState() == DeploymentState.Success &&
						previous.getSuccess() != null) {
					current = commands.get(previous.getSuccess());
				} else if(commandData.getDeploymentState() == DeploymentState.UnSuccessful && 
						previous.getFail() != null) {
					current = commands.get(previous.getFail());
				} else if(commandData.getDeploymentState() == DeploymentState.HasError) {
					return false;
				}
			}
			
			return true;
		}
		
		return false;
	}
}
