/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.services;

import com.microsoft.jenkins.appservice.commands.DeploymentState;
import com.microsoft.jenkins.appservice.commands.IBaseCommandData;
import com.microsoft.jenkins.appservice.commands.ICommand;
import com.microsoft.jenkins.appservice.commands.TransitionInfo;

import java.util.HashMap;

public final class CommandService {

    private CommandService() {
        // Hide
    }

    public static boolean executeCommands(ICommandServiceData commandServiceData) {
        Class startCommand = commandServiceData.getStartCommandClass();
        HashMap<Class, TransitionInfo> commands = commandServiceData.getCommands();
        if (!commands.isEmpty() && startCommand != null) {
            //successfully started
            TransitionInfo current = commands.get(startCommand);
            while (current != null) {
                ICommand<IBaseCommandData> command = current.getCommand();
                IBaseCommandData commandData = commandServiceData.getDataForCommand(command);
                command.execute(commandData);
                TransitionInfo previous = current;
                current = null;

                if (commandData.getDeploymentState() == DeploymentState.Success
                        && previous.getSuccess() != null) {
                    current = commands.get(previous.getSuccess());
                } else if (commandData.getDeploymentState() == DeploymentState.UnSuccessful
                        && previous.getFail() != null) {
                    current = commands.get(previous.getFail());
                } else if (commandData.getDeploymentState() == DeploymentState.HasError) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
