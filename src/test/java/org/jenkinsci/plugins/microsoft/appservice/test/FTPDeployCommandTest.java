/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package org.jenkinsci.plugins.microsoft.appservice.test;

import hudson.FilePath;
import org.jenkinsci.plugins.microsoft.appservice.commands.FTPDeployCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

public class FTPDeployCommandTest {

    @Rule
    public TemporaryFolder workspaceDir = new TemporaryFolder();

    @Test
    public void getRemoteFileName() throws Exception {
        assertGetRemoteFileName("a", "a/b/c.txt", "b/c.txt");
        assertGetRemoteFileName("a", "a/b.txt", "b.txt");
        assertGetRemoteFileName("", "c.txt", "c.txt");
    }

    private void assertGetRemoteFileName(String sourceDirPath, String filePath, String expectedRemoteName) throws Exception {
        final FilePath workspace = new FilePath(workspaceDir.getRoot());
        final FilePath sourceDir = workspace.child(sourceDirPath);
        final FilePath file = workspace.child(filePath);

        final FTPDeployCommand command = new FTPDeployCommand();
        final String remoteName = Whitebox.invokeMethod(command, "getRemoteFileName", sourceDir, file);
        Assert.assertEquals(expectedRemoteName, remoteName);
    }
}
