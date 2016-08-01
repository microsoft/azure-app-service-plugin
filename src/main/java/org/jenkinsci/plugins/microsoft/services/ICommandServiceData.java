package org.jenkinsci.plugins.microsoft.services;

import java.util.Hashtable;

import org.jenkinsci.plugins.microsoft.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.commands.ICommand;
import org.jenkinsci.plugins.microsoft.commands.TransitionInfo;

public interface ICommandServiceData {
	public Class getStartCommandClass();
	public Hashtable<Class, TransitionInfo> getCommands(); 
	public IBaseCommandData getDataForCommand(ICommand command);
}
