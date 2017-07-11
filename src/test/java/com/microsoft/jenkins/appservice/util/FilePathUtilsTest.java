/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.appservice.util;

import hudson.FilePath;
import org.apache.commons.io.FilenameUtils;
import com.microsoft.jenkins.appservice.util.FilePathUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FilePathUtilsTest {

    @Rule
    public TemporaryFolder workspaceDir = new TemporaryFolder();

    @Test
    public void trimDirectoryPrefix() throws Exception {
        assertTrimDirectoryPrefix("a", "a/b/c.txt", "b/c.txt");
        assertTrimDirectoryPrefix("a", "a/b.txt", "b.txt");
        assertTrimDirectoryPrefix("a/", "a/b.txt", "b.txt");
        assertTrimDirectoryPrefix("", "c.txt", "c.txt");
    }

    private void assertTrimDirectoryPrefix(String sourceDirPath, String filePath, String expectedRemoteName) throws Exception {
        final FilePath workspace = new FilePath(workspaceDir.getRoot());
        final FilePath sourceDir = workspace.child(sourceDirPath);
        final FilePath file = workspace.child(filePath);

        final String remoteName = FilePathUtils.trimDirectoryPrefix(sourceDir, file);
        Assert.assertEquals(expectedRemoteName, FilenameUtils.separatorsToUnix(remoteName));
    }
}
