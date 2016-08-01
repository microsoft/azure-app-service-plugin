package org.jenkinsci.plugins.microsoft.commands;

public enum DeploymentState {
	Unknown,
	Done,
	HasError,
	Running,
	Success,
	UnSuccessful,
}
