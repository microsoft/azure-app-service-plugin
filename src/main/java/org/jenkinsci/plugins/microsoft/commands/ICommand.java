package org.jenkinsci.plugins.microsoft.commands;

public interface ICommand<T extends IBaseCommandData>  {
	public void execute(T context);
}
