package com.github.godfather1103.gradle.entity

import com.github.dockerjava.api.exception.DockerException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import java.io.IOException
import java.util.*

class Git {

    var repo: Optional<Repository> = Optional.empty()

    constructor() {
        val builder = FileRepositoryBuilder()
        builder.readEnvironment()
        builder.findGitDir()
        if (builder.gitDir != null) {
            repo = Optional.ofNullable(builder.build())
        }
    }

    fun isRepository(): Boolean {
        return repo.isPresent
    }

    @Throws(GitAPIException::class, DockerException::class, IOException::class, GradleException::class)
    fun getCommitId(): String {
        if (!repo.isPresent) {
            throw GradleException(
                "Cannot tag with git commit ID because directory not a git repo"
            )
        }
        val result = StringBuilder()
        repo.get().use {
            val head = Optional.ofNullable(it.resolve("HEAD"))
                .map { i -> i.name }
                .orElse("")
            if (head.isEmpty()) {
                return ""
            }
            result.append(head.substring(0, 8))
            val git = org.eclipse.jgit.api.Git(it)
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
        }
        return if (result.isEmpty()) "" else result.toString()
    }
}