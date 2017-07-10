/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.services;

import com.microsoft.jenkins.appservice.commands.IBaseCommandData;
import com.microsoft.jenkins.appservice.commands.ICommand;
import com.microsoft.jenkins.appservice.commands.TransitionInfo;

import java.util.HashMap;

public interface ICommandServiceData {

    Class getStartCommandClass();

    HashMap<Class, TransitionInfo> getCommands();

    IBaseCommandData getDataForCommand(ICommand command);
}
