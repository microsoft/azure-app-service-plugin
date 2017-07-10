/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.util;

import hudson.FilePath;

public final class FilePathUtils {

    private FilePathUtils() {
        // Hide
    }

    public static String trimDirectoryPrefix(FilePath dir, FilePath file) {
        final String prefix = dir.getRemote();
        final String filePath = file.getRemote();
        if (filePath.startsWith(prefix)) {
            return filePath.substring(prefix.length() + 1);
        } else {
            return filePath;
        }
    }
}
