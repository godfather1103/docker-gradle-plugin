package com.github.godfather1103.gradle.entity;

import com.spotify.docker.client.exceptions.DockerException;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.GradleException;

import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2022</p>
 * <p>Company:      https://github.com/godfather1103</p>
 *
 * @author 作者: Jack Chu E-mail: chuchuanbao@gmail.com<br/>
 * <p>
 * 创建时间：2022/12/13 17:14
 * @version 1.0
 * @since 1.0
 */
public class Git {

    private Repository repo;

    public Git() throws IOException {
        final FileRepositoryBuilder builder = new FileRepositoryBuilder();
        // scan environment GIT_* variables
        builder.readEnvironment();
        // scan up the file system tree
        builder.findGitDir();
        // if getGitDir is null, then we are not in a git repository
        repo = builder.getGitDir() == null ? null : builder.build();
    }

    public boolean isRepository() {
        return repo != null;
    }

    public Repository getRepo() {
        return repo;
    }

    void setRepo(final Repository repo) {
        this.repo = repo;
    }

    public String getCommitId()
            throws GitAPIException, DockerException, IOException, GradleException {

        if (repo == null) {
            throw new GradleException(
                    "Cannot tag with git commit ID because directory not a git repo");
        }

        final StringBuilder result = new StringBuilder();

        try {
            // get the first 7 characters of the latest commit
            final ObjectId head = repo.resolve("HEAD");
            if (head == null || isNullOrEmpty(head.getName())) {
                return null;
            }

            result.append(head.getName().substring(0, 7));
            final org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);

            // append first git tag we find
            for (final Ref gitTag : git.tagList().call()) {
                if (gitTag.getObjectId().equals(head)) {
                    // name is refs/tag/name, so get substring after last slash
                    final String name = gitTag.getName();
                    result.append(".");
                    result.append(name.substring(name.lastIndexOf('/') + 1));
                    break;
                }
            }

            // append '.DIRTY' if any files have been modified
            final Status status = git.status().call();
            if (status.hasUncommittedChanges()) {
                result.append(".DIRTY");
            }
        } finally {
            repo.close();
        }

        return result.length() == 0 ? null : result.toString();
    }

}
