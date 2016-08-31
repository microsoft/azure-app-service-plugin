/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.commands;

public enum DeploymentState {
	Unknown,
	Done,
	HasError,
	Running,
	Success,
	UnSuccessful,
}
