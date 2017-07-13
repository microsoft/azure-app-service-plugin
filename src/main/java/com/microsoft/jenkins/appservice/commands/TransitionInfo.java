/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

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

    public TransitionInfo(final ICommand command, final Class success, final Class fail) {
        this.command = command;
        this.success = success;
        this.fail = fail;
    }
}
