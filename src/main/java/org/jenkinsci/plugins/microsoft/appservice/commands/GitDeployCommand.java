/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package org.jenkinsci.plugins.microsoft.appservice.commands;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.microsoft.azure.management.appservice.PublishingProfile;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitTool;
import hudson.remoting.VirtualChannel;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.RepositoryCallback;
import org.jenkinsci.plugins.microsoft.appservice.util.FilePathUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public class GitDeployCommand implements ICommand<GitDeployCommand.IGitDeployCommandData> {

    private static final String DEPLOY_REPO = ".azure-deploy";
    private static final String DEPLOY_COMMIT_MESSAGE = "Deploy ${BUILD_TAG}";
    private static final String DEPLOY_BRANCH = "master";
    private static final String DEPLOY_REMOTE_BRANCH = "origin/" + DEPLOY_BRANCH;

    @Override
    public void execute(IGitDeployCommandData context) {
        try {
            final PublishingProfile pubProfile = context.getPublishingProfile();
            final Run run = context.getRun();
            final TaskListener listener = context.getListener();
            final EnvVars env = run.getEnvironment(listener);
            final FilePath ws = context.getWorkspace();
            if (ws == null) {
                context.logError("Workspace is null");
                context.setDeploymentState(DeploymentState.HasError);
                return;
            }
            final FilePath repo = ws.child(DEPLOY_REPO);
            final String gitExe = getGitExe(run, listener);

            GitClient git = Git.with(listener, env)
                .in(repo)
                .using(gitExe)
                .getClient();

            git.addCredentials(pubProfile.gitUrl(), new UsernamePasswordCredentialsImpl(
                    CredentialsScope.SYSTEM, "", "", pubProfile.gitUsername(), pubProfile.gitPassword()));

            git.clone_().url(pubProfile.gitUrl()).execute();

            // Sometimes remote repository is bare and the master branch doesn't exist
            Set<Branch> branches = git.getRemoteBranches();
            for (Branch branch : branches) {
                if (branch.getName().equals(DEPLOY_REMOTE_BRANCH)) {
                    git.checkout().ref(DEPLOY_BRANCH).execute();
                    break;
                }
            }

            cleanWorkingDirectory(git);

            final FilePath sourceDir = ws.child(Util.fixNull(context.getSourceDirectory()));
            final String targetDir = Util.fixNull(context.getTargetDirectory());
            copyAndAddFiles(git, repo, sourceDir, targetDir, context.getFilePath());

            if (!isWorkingTreeChanged(git)) {
                context.logStatus("Deploy repository is up-to-date. Nothing to commit.");
                context.setDeploymentState(DeploymentState.Success);
                return;
            }

            git.commit(env.expand(DEPLOY_COMMIT_MESSAGE));

            git.push().to(new URIish(pubProfile.gitUrl())).execute();

            context.setDeploymentState(DeploymentState.Success);

        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            context.logError("Fail to deploy using Git: " + e.getMessage());
            context.setDeploymentState(DeploymentState.HasError);
        }
    }

    private String getGitExe(Run run, TaskListener listener) throws IOException, InterruptedException {
        GitTool tool = GitTool.getDefaultInstallation();

        final EnvVars env = run.getEnvironment(listener);
        if (env != null) {
            tool = tool.forEnvironment(env);
        }

        Node node = null;
        if (run instanceof AbstractBuild) {
            node = ((AbstractBuild) run).getBuiltOn();
        }
        if (node != null) {
            tool = tool.forNode(node, listener);
        }

        return tool.getGitExe();
    }

    /**
     * Remove all existing files in the working directory, from both git and disk
     *
     * This method is modified from RmCommand in JGit.
     *
     * @param git Git client
     * @throws IOException
     * @throws InterruptedException
     */
    private void cleanWorkingDirectory(GitClient git) throws IOException, InterruptedException {
        git.withRepository(new CleanWorkingDirectoryCallback());
    }

    private static final class CleanWorkingDirectoryCallback implements RepositoryCallback<Void> {
        @Override
        public Void invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
            DirCache dc = null;

            try (final TreeWalk tw = new TreeWalk(repo)) {
                dc = repo.lockDirCache();
                DirCacheBuilder builder = dc.builder();
                tw.reset(); // drop the first empty tree, which we do not need here
                tw.setRecursive(true);
                tw.setFilter(TreeFilter.ALL);
                tw.addTree(new DirCacheBuildIterator(builder));

                while (tw.next()) {
                    final FileMode mode = tw.getFileMode(0);
                    if (mode.getObjectType() == Constants.OBJ_BLOB) {
                        final File path = new File(repo.getWorkTree(),
                                tw.getPathString());
                        // Deleting a blob is simply a matter of removing
                        // the file or symlink named by the tree entry.
                        delete(repo, path);
                    }
                }
                builder.commit();
            } finally {
                if (dc != null) {
                    dc.unlock();
                }
            }

            return null;
        }

        private void delete(Repository repo, File p) {
            while (p != null && !p.equals(repo.getWorkTree()) && p.delete()) {
                p = p.getParentFile();
            }
        }
    }

    /**
     * Copy selected files to git working directory and stage them.
     *
     * @param git Git client
     * @param repo Path to git repo
     * @param sourceDir Source directory
     * @param targetDir Target directory
     * @param filesPattern Files name pattern
     * @throws IOException
     * @throws InterruptedException
     */
    private void copyAndAddFiles(GitClient git, FilePath repo, FilePath sourceDir, String targetDir, String filesPattern)
            throws IOException, InterruptedException {
        final FilePath[] files = sourceDir.list(filesPattern);
        for (final FilePath file: files) {
            final String fileName = FilePathUtils.trimDirectoryPrefix(sourceDir, file);
            FilePath repoPath = new FilePath(repo.child(targetDir), fileName);
            file.copyTo(repoPath);

            // Git always use Unix file path
            String filePathInGit = FilenameUtils.separatorsToUnix(FilenameUtils.concat(targetDir, fileName));
            git.add(filePathInGit);
        }
    }

    /**
     * Check if working tree changed.
     *
     * @param git Git client
     * @return If working tree changed
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean isWorkingTreeChanged(GitClient git) throws IOException, InterruptedException {
        return git.withRepository(new IsWorkingTreeChangedCallback());
    }

    private static final class IsWorkingTreeChangedCallback implements RepositoryCallback<Boolean> {
        @Override
        public Boolean invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
            FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
            IndexDiff diff = new IndexDiff(repo, Constants.HEAD, workingTreeIt);
            return diff.diff();
        }
    }

    public interface IGitDeployCommandData extends IBaseCommandData {

        PublishingProfile getPublishingProfile();

        String getFilePath();

        String getSourceDirectory();

        String getTargetDirectory();
    }
}
