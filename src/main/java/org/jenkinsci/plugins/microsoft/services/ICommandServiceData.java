/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.services;

import org.jenkinsci.plugins.microsoft.appservice.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.appservice.commands.ICommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TransitionInfo;

import java.util.HashMap;

public interface ICommandServiceData {

    Class getStartCommandClass();

    HashMap<Class, TransitionInfo> getCommands();

    IBaseCommandData getDataForCommand(ICommand command);
}
