/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.jenkinsci.plugins.microsoft.exceptions.AzureCloudException;

public interface IARMTemplateServiceData {

    public IAzureConnectionData getAzureConnectionData();

    public String getResourceGroupName();

    public String getEmbeddedTemplateName();

    public void configureTemplate(JsonNode tmp) throws IllegalAccessException, AzureCloudException;
}
