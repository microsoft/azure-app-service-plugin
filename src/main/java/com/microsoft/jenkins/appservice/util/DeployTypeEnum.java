/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.util;

public enum DeployTypeEnum {
    GIT("GIT"),
    WAR("WAR"),
    ZIP("ZIP");

    private String label;

    DeployTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
