/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.services;

import org.jenkinsci.plugins.microsoft.appservice.commands.DeploymentState;
import org.jenkinsci.plugins.microsoft.appservice.commands.IBaseCommandData;
import org.jenkinsci.plugins.microsoft.appservice.commands.ICommand;
import org.jenkinsci.plugins.microsoft.appservice.commands.TransitionInfo;

import java.util.HashMap;

public class CommandService {

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
