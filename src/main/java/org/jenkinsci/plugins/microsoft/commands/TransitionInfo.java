package org.jenkinsci.plugins.microsoft.commands;

public class TransitionInfo {
	private ICommand<IBaseCommandData> command;
	private Class success;
	private Class fail;
	
	public ICommand<IBaseCommandData> getCommand() {
		return this.command;
	}
	
	public Class getSuccess() {
		return this.success;
	}

	public Class getFail() {
		return this.fail;
	}
	
	public TransitionInfo(ICommand command, Class success, Class fail) {
		this.command = command;
		this.success = success;
		this.fail = fail;
	}
}