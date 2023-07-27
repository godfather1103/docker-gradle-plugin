package com.github.godfather1103.gradle.entity

import org.junit.Test

class GitTest {

    @Test
    fun testGit() {
        val git = Git()
        if (git.isRepository()) {
            println(git.getCommitId())
        }
    }
}