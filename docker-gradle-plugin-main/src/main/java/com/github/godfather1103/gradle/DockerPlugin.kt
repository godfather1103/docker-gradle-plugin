package com.github.godfather1103.gradle

import com.github.godfather1103.gradle.ext.DockerPluginExtension
import com.github.godfather1103.gradle.tasks.BuildMojo
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2022</p>
 * <p>Company:      https://github.com/godfather1103</p>
 *
 * @author 作者: Jack Chu E-mail: chuchuanbao@gmail.com<br/>
 *
 * 创建时间：2022/12/13 17:14
 * @version 1.0
 * @since 1.0
 */
class DockerPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        println("\u6fc0\u6d3b\u4e86\u63d2\u4ef6")
        val docker = target.extensions.create(
            "docker",
            DockerPluginExtension::class.java,
            target
        )
        val dockerBuild = target.tasks.register("dockerBuild")
        dockerBuild.configure {
            it.group = "docker"
            docker.dockerBuildDependsOn.isPresent
            val dockerBuildDependsOn = docker.dockerBuildDependsOn.get()
            if (dockerBuildDependsOn.isNotEmpty()) {
                it.dependsOn(dockerBuildDependsOn.toTypedArray())
            }
            val buildMojo = BuildMojo(docker)
            it.doLast { buildMojo.execute() }
        }
    }
}