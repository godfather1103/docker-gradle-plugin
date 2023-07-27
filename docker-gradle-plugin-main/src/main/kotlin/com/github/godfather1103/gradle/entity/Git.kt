package com.github.godfather1103.gradle.entity

import com.github.dockerjava.api.exception.DockerException
import com.google.common.base.Strings
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import java.io.IOException

class Git {

    var repo: Repository?

    constructor() {
        val builder = FileRepositoryBuilder()
        builder.readEnvironment()
        builder.findGitDir()
        repo = if (builder.gitDir == null) {
            null
        } else {
            builder.build()
        }
    }

    fun isRepository(): Boolean {
        return repo != null
    }

    @Throws(GitAPIException::class, DockerException::class, IOException::class, GradleException::class)
    fun getCommitId(): String? {
        if (repo == null) {
            throw GradleException(
                "Cannot tag with git commit ID because directory not a git repo"
            )
        }
        val result = StringBuilder()
        try {
            // get the first 7 characters of the latest commit
            val head = repo!!.resolve("HEAD")
            if (head == null || Strings.isNullOrEmpty(head.name)) {
                return null
            }
            result.append(head.name.substring(0, 8))
            val git = org.eclipse.jgit.api.Git(repo)
            // append first git tag we find
            for (gitTag in git.tagList().call()) {
                if (gitTag.objectId.equals(head)) {
                    // name is refs/tag/name, so get substring after last slash
                    val name = gitTag.name
                    result.append(".")
                    result.append(name.substring(name.lastIndexOf('/') + 1))
                    break
                }
            }
            // append '.DIRTY' if any files have been modified
            val status = git.status().call()
            if (status.hasUncommittedChanges()) {
                result.append(".DIRTY")
            }
        } finally {
            repo!!.close()
        }
        return if (result.isEmpty()) null else result.toString()
    }
}