/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.test;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.RepositoryCallback;
import org.jenkinsci.plugins.microsoft.appservice.commands.GitDeployCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class GitDeployCommandTest {

    @Rule
    public TemporaryFolder workspace = new TemporaryFolder();

    @Test
    public void cleanWorkingDirectory() throws Exception {
        GitDeployCommand command = new GitDeployCommand();
        File repo = workspace.newFolder("repo");
        GitClient git = Git.with(null, null)
                .in(repo)
                .getClient();
        git.init();
        FileUtils.write(new File(repo, "f1.txt"), "f1");
        FileUtils.write(new File(repo, "f2.txt"), "f2");
        File deepDir = new File(repo, "deep");
        deepDir.mkdir();
        FileUtils.write(new File(deepDir, "f3.txt"), "f3");

        git.add("f1.txt");
        git.add("f2.txt");
        git.add("deep/f3.txt");

        git.commit("c1");

        Whitebox.invokeMethod(command, "cleanWorkingDirectory", git);

        // Files on disk should be removed
        Assert.assertFalse(new File(repo, "f1.txt").exists());
        Assert.assertFalse(new File(repo, "f2.txt").exists());
        Assert.assertFalse(new File(deepDir, "f3.txt").exists());

        // Files in Git should be removed
        git.withRepository(new RepositoryCallback<Void>() {
            @Override
            public Void invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
                FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
                IndexDiff diff = new IndexDiff(repo, Constants.HEAD, workingTreeIt);
                diff.diff();
                Set<String> removed = diff.getRemoved();

                Assert.assertTrue(removed.contains("f1.txt"));
                Assert.assertTrue(removed.contains("f2.txt"));
                Assert.assertTrue(removed.contains("deep/f3.txt"));

                return null;
            }
        });
    }

    @Test
    public void copyAndAddFiles() throws Exception {
        GitDeployCommand command = new GitDeployCommand();
        File repo = workspace.newFolder("repo");
        GitClient git = Git.with(null, null)
                .in(repo)
                .getClient();
        git.init();
        File src = workspace.newFolder("src");
        FileUtils.write(new File(src, "f1.txt"), "f1");
        FileUtils.write(new File(src, "f2.txt"), "f2");
        File deepDir = new File(src, "deep");
        deepDir.mkdir();
        FileUtils.write(new File(deepDir, "f3.txt"), "f3");
        FileUtils.write(new File(src, "exclude.bak"), "exclude");

        Whitebox.invokeMethod(command, "copyAndAddFiles",
            git, new FilePath(src), new FilePath(repo), "**/*.txt", "");

        // Files should be copied
        Assert.assertTrue(new File(repo, "f1.txt").exists());
        Assert.assertTrue(new File(repo, "f2.txt").exists());
        Assert.assertTrue(new File(deepDir, "f3.txt").exists());
        Assert.assertFalse(new File(repo, "exclude.bak").exists());

        // Files should be staged
        git.withRepository(new RepositoryCallback<Void>() {
            @Override
            public Void invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
                FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
                IndexDiff diff = new IndexDiff(repo, Constants.HEAD, workingTreeIt);
                diff.diff();
                Set<String> added = diff.getAdded();

                Assert.assertTrue(added.contains("f1.txt"));
                Assert.assertTrue(added.contains("f2.txt"));
                Assert.assertTrue(added.contains("deep/f3.txt"));
                Assert.assertFalse(added.contains("exclude.bak"));

                return null;
            }
        });
    }

    @Test
    public void copyAndAddFilesWithTargetDirectory() throws Exception {
        GitDeployCommand command = new GitDeployCommand();
        File repo = workspace.newFolder("repo");
        GitClient git = Git.with(null, null)
                .in(repo)
                .getClient();
        git.init();
        File src = workspace.newFolder("src");
        FileUtils.write(new File(src, "f1.txt"), "f1");
        FileUtils.write(new File(src, "f2.txt"), "f2");
        File deepDir = new File(src, "deep");
        deepDir.mkdir();
        FileUtils.write(new File(deepDir, "f3.txt"), "f3");
        FileUtils.write(new File(src, "exclude.bak"), "exclude");

        Whitebox.invokeMethod(command, "copyAndAddFiles",
                git, new FilePath(src), new FilePath(repo), "**/*.txt", "target");

        File targetDir = new File(repo, "target");
        File targetDeepDir = new File(targetDir, "deep");

        // Files should be copied
        Assert.assertTrue(new File(targetDir, "f1.txt").exists());
        Assert.assertTrue(new File(targetDir, "f2.txt").exists());
        Assert.assertTrue(new File(targetDeepDir, "f3.txt").exists());
        Assert.assertFalse(new File(targetDir, "exclude.bak").exists());

        // Files should be staged
        git.withRepository(new RepositoryCallback<Void>() {
            @Override
            public Void invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
                FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
                IndexDiff diff = new IndexDiff(repo, Constants.HEAD, workingTreeIt);
                diff.diff();
                Set<String> added = diff.getAdded();

                Assert.assertTrue(added.contains("target/f1.txt"));
                Assert.assertTrue(added.contains("target/f2.txt"));
                Assert.assertTrue(added.contains("target/deep/f3.txt"));
                Assert.assertFalse(added.contains("target/exclude.bak"));

                return null;
            }
        });
    }

    @Test
    public void isWorkingTreeChanged() throws Exception {
        GitDeployCommand command = new GitDeployCommand();
        GitClient git = Git.with(null, null)
                .in(workspace.getRoot())
                .getClient();
        git.init();

        // Create a file
        FileUtils.write(workspace.newFile("f1.txt"), "f1");

        boolean changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertTrue(changed);

        // Stage a file
        git.add("f1.txt");
        changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertTrue(changed);

        // Commit
        git.commit("c1");
        changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertFalse(changed);

        // Remove it
        new File(workspace.getRoot(), "f1.txt").delete();
        changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertTrue(changed);

        // Revert
        git.clean();
        changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertFalse(changed);

        // Change content
        FileUtils.write(new File(workspace.getRoot(), "f1.txt"), "f1-changed");
        changed = Whitebox.<Boolean>invokeMethod(command, "isWorkingTreeChanged", git);
        Assert.assertTrue(changed);
    }


}
