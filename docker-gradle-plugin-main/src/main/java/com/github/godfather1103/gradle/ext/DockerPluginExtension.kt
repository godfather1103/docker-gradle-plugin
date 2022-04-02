package com.github.godfather1103.gradle.ext

import com.github.godfather1103.gradle.entity.AuthConfig
import com.github.godfather1103.gradle.entity.Resource
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class DockerPluginExtension(val project: Project) {

    val dockerHost: Property<String> = project.objects.property(String::class.java)

    val dockerCertPath: Property<String> = project.objects.property(String::class.java)

    val serverId: Property<String> = project.objects.property(String::class.java)

    val registryUrl: Property<String> = project.objects.property(String::class.java)

    val retryPushCount: Property<Int> = project.objects.property(Int::class.java).value(5)

    val retryPushTimeout: Property<Int> = project.objects.property(Int::class.java).value(10000)

    val skipDocker: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val skipDockerPush: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val dockerDirectory: Property<String> = project.objects.property(String::class.java)

    val skipDockerBuild: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val pullOnBuild: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val noCache: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val rm: Property<Boolean> = project.objects.property(Boolean::class.java).value(true)

    val saveImageToTarArchive: Property<String> = project.objects.property(String::class.java)

    val pushImage: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val pushImageTag: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val forceTags: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val dockerMaintainer: Property<String> = project.objects.property(String::class.java)

    val dockerBaseImage: Property<String> = project.objects.property(String::class.java)

    val dockerEntryPoint: Property<String> = project.objects.property(String::class.java)

    val dockerVolumes: ListProperty<String> = project.objects.listProperty(String::class.java)

    val dockerLabels: ListProperty<String> = project.objects.listProperty(String::class.java)

    val dockerCmd: Property<String> = project.objects.property(String::class.java)

    val workdir: Property<String> = project.objects.property(String::class.java)

    val user: Property<String> = project.objects.property(String::class.java)

    val dockerRuns: ListProperty<String> = project.objects.listProperty(String::class.java)

    val squashRunCommands: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val buildDirectory: String = project.buildDir.absolutePath

    val dockerBuildProfile: Property<String> = project.objects.property(String::class.java)

    val useGitCommitId: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val dockerImageTags: ListProperty<String> = project.objects.listProperty(String::class.java)

    val dockerDefaultBuildProfile: Property<String> = project.objects.property(String::class.java)

    val dockerEnv: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)

    val dockerExposes: ListProperty<String> = project.objects.listProperty(String::class.java)

    val dockerBuildArgs: MapProperty<String, String> =
        project.objects.mapProperty(String::class.java, String::class.java)

    val healthcheck: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)

    val network: Property<String> = project.objects.property(String::class.java)

    val imageName: Property<String> = project.objects.property(String::class.java)

    val removeAllTags: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val image: Property<String> = project.objects.property(String::class.java)

    val skipDockerTag: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    val newName: Property<String> = project.objects.property(String::class.java)

    val tagInfoFile: Property<String> =
        project.objects.property(String::class.java).value("$buildDirectory/image_info.json")


    /**
     * 认证信息
     * */
    val auth: Property<AuthConfig> = project.objects.property(AuthConfig::class.java)

    /**
     * tags
     * */
    val tags: SetProperty<String> = project.objects.setProperty(String::class.java)

    val dockerBuildDependsOn: SetProperty<String> = project.objects.setProperty(String::class.java)

    val resources: ListProperty<Resource> = project.objects.listProperty(Resource::class.java)

}