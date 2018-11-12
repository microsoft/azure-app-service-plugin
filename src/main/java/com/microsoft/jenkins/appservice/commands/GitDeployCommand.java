/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.appservice.commands;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.appservice.AzureAppServicePlugin;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.appservice.util.FilePathUtils;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
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
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.RepositoryCallback;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public class GitDeployCommand implements ICommand<GitDeployCommand.IGitDeployCommandData> {

    private static final String DEPLOY_REPO = ".azure-deploy";
    private static final String DEPLOY_COMMIT_MESSAGE = "Deploy ${BUILD_TAG}";
    private static final String DEPLOY_BRANCH = "master";
    private static final String DEPLOY_REMOTE_BRANCH = "origin/" + DEPLOY_BRANCH;
    private static final String GIT_ADD_ALL_PARAMETER = ".";

    @Override
    public void execute(final IGitDeployCommandData context) {
        final JobContext jobContext = context.getJobContext();
        try {
            final PublishingProfile pubProfile = context.getPublishingProfile();
            final Run run = jobContext.getRun();
            final TaskListener listener = jobContext.getTaskListener();
            final EnvVars env = run.getEnvironment(listener);
            final FilePath ws = jobContext.getWorkspace();
            if (ws == null) {
                context.logError("Workspace is null");
                context.setCommandState(CommandState.HasError);
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
            final String filePath = Util.fixNull(context.getFilePath());
            copyAndAddFiles(git, repo, sourceDir, targetDir, filePath);

            if (!isWorkingTreeChanged(git)) {
                context.logStatus("Deploy repository is up-to-date. Nothing to commit.");
                context.setCommandState(CommandState.Success);
                return;
            }

            setAuthor(git);
            setCommitter(git);

            git.commit(env.expand(DEPLOY_COMMIT_MESSAGE));

            git.push().ref(DEPLOY_BRANCH + ":" + DEPLOY_BRANCH).to(new URIish(pubProfile.gitUrl())).execute();
            context.logStatus(String.format("Deploy to app with default host https://%s",
                    context.getWebAppBase().defaultHostName()));

            context.setCommandState(CommandState.Success);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_GIT_DEPLOY,
                    "Branch", DEPLOY_BRANCH,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebAppBase().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebAppBase().name()));
        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            context.logError("Fail to deploy using Git: " + e.getMessage());
            context.setCommandState(CommandState.HasError);
            AzureAppServicePlugin.sendEvent(Constants.AI_WEB_APP, Constants.AI_GIT_DEPLOY_FAILED,
                    "Branch", DEPLOY_BRANCH,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebAppBase().resourceGroupName()),
                    "WebApp", AppInsightsUtils.hash(context.getWebAppBase().name()),
                    "Message", e.getMessage());
        }
    }

    private String getGitExe(final Run run, final TaskListener listener) throws IOException, InterruptedException {
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
     * Set author according to current git config.
     *
     * @param git Git client
     * @throws IOException
     * @throws InterruptedException
     */
    private void setAuthor(final GitClient git) throws IOException, InterruptedException {
        final PersonIdent identity = git.withRepository(new GetAuthorCallback());
        git.setAuthor(identity);
    }

    private static final class GetAuthorCallback implements RepositoryCallback<PersonIdent>  {
        @Override
        public PersonIdent invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
            final StoredConfig config = repo.getConfig();
            final UserConfig userConfig = UserConfig.KEY.parse(config);
            return new PersonIdent(userConfig.getAuthorName(), userConfig.getAuthorEmail());
        }
    }

    /**
     * Set committer according to current git config.
     *
     * @param git Git client
     * @throws IOException
     * @throws InterruptedException
     */
    private void setCommitter(final GitClient git) throws IOException, InterruptedException {
        final PersonIdent identity = git.withRepository(new GetCommitterCallback());
        git.setCommitter(identity);
    }

    private static final class GetCommitterCallback implements RepositoryCallback<PersonIdent>  {
        @Override
        public PersonIdent invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {
            final StoredConfig config = repo.getConfig();
            final UserConfig userConfig = UserConfig.KEY.parse(config);
            return new PersonIdent(userConfig.getCommitterName(), userConfig.getCommitterEmail());
        }
    }

    /**
     * Remove all existing files in the working directory, from both git and disk
     * <p>
     * This method is modified from RmCommand in JGit.
     *
     * @param git Git client
     * @throws IOException
     * @throws InterruptedException
     */
    private void cleanWorkingDirectory(final GitClient git) throws IOException, InterruptedException {
        git.withRepository(new CleanWorkingDirectoryCallback());
    }

    private static final class CleanWorkingDirectoryCallback implements RepositoryCallback<Void> {
        @Override
        public Void invoke(final Repository repo, final VirtualChannel channel)
                throws IOException, InterruptedException {
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
                    if (mode.getObjectType() == org.eclipse.jgit.lib.Constants.OBJ_BLOB) {
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

        private void delete(final Repository repo, final File target) {
            File cur = target;
            while (cur != null && !cur.equals(repo.getWorkTree()) && cur.delete()) {
                cur = cur.getParentFile();
            }
        }
    }

    /**
     * Copy selected files to git working directory and stage them.
     *
     * @param git          Git client
     * @param repo         Path to git repo
     * @param sourceDir    Source directory
     * @param targetDir    Target directory
     * @param filesPattern Files name pattern
     * @throws IOException
     * @throws InterruptedException
     */
    private void copyAndAddFiles(
            final GitClient git,
            final FilePath repo,
            final FilePath sourceDir,
            final String targetDir,
            final String filesPattern) throws IOException, InterruptedException {
        final FilePath[] files = sourceDir.list(filesPattern);
        for (final FilePath file : files) {
            final String fileName = FilePathUtils.trimDirectoryPrefix(sourceDir, file);
            FilePath repoPath = new FilePath(repo.child(targetDir), fileName);
            file.copyTo(repoPath);
        }
        git.add(GIT_ADD_ALL_PARAMETER);
    }

    /**
     * Check if working tree changed.
     *
     * @param git Git client
     * @return If working tree changed
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean isWorkingTreeChanged(final GitClient git) throws IOException, InterruptedException {
        return git.withRepository(new IsWorkingTreeChangedCallback());
    }

    private static final class IsWorkingTreeChangedCallback implements RepositoryCallback<Boolean> {
        @Override
        public Boolean invoke(final Repository repo, final VirtualChannel channel)
                throws IOException, InterruptedException {
            FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
            IndexDiff diff = new IndexDiff(repo, org.eclipse.jgit.lib.Constants.HEAD, workingTreeIt);
            return diff.diff();
        }
    }

    public interface IGitDeployCommandData extends IBaseCommandData {

        PublishingProfile getPublishingProfile();

        String getFilePath();

        String getSourceDirectory();

        String getTargetDirectory();

        WebAppBase getWebAppBase();
    }
}
