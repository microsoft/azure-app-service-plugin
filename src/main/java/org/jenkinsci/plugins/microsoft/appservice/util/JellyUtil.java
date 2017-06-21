/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.util;

import java.util.Calendar;
import java.util.UUID;

public final class JellyUtil {

    private JellyUtil() {
        // Hide
    }

    public static String generateUniqueName(String prefix) {
        String dnsNamePrefix = UUID.randomUUID().toString().replace("-", "");
        long millis = Calendar.getInstance().getTimeInMillis();
        long datePart = millis % 1000000000;
        return prefix + dnsNamePrefix.toLowerCase().substring(0, 8) + datePart;
    }
}
