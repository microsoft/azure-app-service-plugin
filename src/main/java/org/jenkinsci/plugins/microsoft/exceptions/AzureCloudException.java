/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.exceptions;

public class AzureCloudException extends Exception {

    public AzureCloudException(final String message) {
        super(message);
    }

    public AzureCloudException() {
        super();
    }

    public AzureCloudException(final String msg, final Exception excep) {
        super(msg, excep);
    }

    public AzureCloudException(final Exception excep) {
        super(excep);
    }

    private static final long serialVersionUID = -8157417759485046943L;

}
