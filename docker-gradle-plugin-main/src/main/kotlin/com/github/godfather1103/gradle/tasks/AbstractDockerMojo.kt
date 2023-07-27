package com.github.godfather1103.gradle.tasks

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.github.godfather1103.gradle.entity.AuthConfig
import com.github.godfather1103.gradle.ext.DockerPluginExtension
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2022</p>
 * <p>Company:      https://github.com/godfather1103</p>
 * 基础的运行
 *
 * @author 作者: Jack Chu E-mail: chuchuanbao@gmail.com
 * <p>
 * 创建时间：2022/3/18 21:31
 * @version 1.0
 * @since 1.0
 */
abstract class AbstractDockerMojo(val ext: DockerPluginExtension) : Action<DockerClient> {

    private val factory = NettyDockerCmdExecFactory()

    private val log = LoggerFactory.getLogger(AbstractDockerMojo::class.java)

    open fun getLog(): Logger {
        return log
    }

    /**
     * 根据扩展配置进行初始化<BR>
     * @author  作者: Jack Chu E-mail: chuchuanbao@gmail.com
     * @date 创建时间：2023/7/27 20:10
     * @param ext 扩展配置
     */
    abstract fun initExt(ext: DockerPluginExtension)

    fun isSkipDocker(): Boolean {
        return ext.skipDocker.getOrElse(false)
    }

    fun getRetryPushTimeout(): Int {
        return ext.retryPushTimeout.getOrElse(10000)
    }

    fun getReadTimeout(): Int {
        return ext.readTimeout.getOrElse(0)
    }

    fun getRetryPushCount(): Int {
        return ext.retryPushCount.getOrElse(5)
    }

    fun isSkipDockerPush(): Boolean {
        return ext.skipDocker.getOrElse(false)
    }

    open fun execute() {
        if (isSkipDocker()) {
            getLog().info("Skipping docker goal")
            return
        }
        // 读取最新值
        initExt(ext)
        try {
            buildDockerClient().use { client -> execute(client) }
        } catch (e: Exception) {
            throw GradleException("Exception caught", e)
        }
    }

    private fun rawDockerHost(): String? {
        return ext.dockerHost.getOrNull()
    }

    private fun dockerCertificates(): Optional<String> {
        val dockerCertPath = ext.dockerCertPath.getOrNull()
        return if (StringUtils.isNotEmpty(dockerCertPath)) {
            Optional.ofNullable(ext.dockerCertPath.getOrNull())
        } else {
            Optional.empty()
        }
    }

    private fun makeAuthConfig(): Optional<AuthConfig> {
        // 1、首先从项目的构建脚本中读取
        var authConfig = ext.auth.getOrNull()
        // 2、没有的话看gradle.properties文件中是否有相关配置
        if (authConfig == null) {
            var obj = ext.project.findProperty("docker.username")
            val username = obj?.toString() ?: ""
            obj = ext.project.findProperty("docker.password")
            val password = obj?.toString() ?: ""
            obj = ext.project.findProperty("docker.email")
            val email = obj?.toString() ?: ""
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                authConfig = AuthConfig(username, password, email)
            }
        }
        // 3、最后环境看一下环境变量中是否有相关配置
        if (authConfig == null) {
            val username = StringUtils.trimToEmpty(System.getenv("docker.username"))
            val password = StringUtils.trimToEmpty(System.getenv("docker.password"))
            val email = StringUtils.trimToEmpty(System.getenv("docker.email"))
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                authConfig = AuthConfig(username, password, email)
            }
        }
        return Optional.ofNullable(authConfig)
    }

    @Throws(GradleException::class)
    private fun registryAuth(configBuilder: DefaultDockerClientConfig.Builder) {
        val authConfig: Optional<AuthConfig> = makeAuthConfig()
        if (authConfig.isPresent) {
            val username = authConfig.get().username
            val password = authConfig.get().password
            val email = authConfig.get().email
            if (StringUtils.isNotEmpty(username)) {
                configBuilder.withRegistryUsername(username)
            }
            if (StringUtils.isNotEmpty(email)) {
                configBuilder.withRegistryEmail(email)
            }
            if (StringUtils.isNotEmpty(password)) {
                configBuilder.withRegistryPassword(password)
            }
            configBuilder.withRegistryUrl(serverIdFor())
        } else {
            // settings.xml has no entry for the configured serverId, warn the user
            getLog().warn("No entry found, cannot configure authentication for that registry")
        }
    }

    private fun serverIdFor(): String {
        val serverId = ext.serverId.getOrNull()
        if (StringUtils.isNotEmpty(serverId)) {
            return serverId!!
        }
        val registryUrl = ext.registryUrl.getOrNull()
        return if (StringUtils.isNotEmpty(registryUrl)) {
            ext.registryUrl.get()
        } else "index.docker.io"
    }

    @Throws(GradleException::class)
    private fun buildDockerClient(): DockerClient {
        val configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        val dockerHost = rawDockerHost()
        if (StringUtils.isNotEmpty(dockerHost)) {
            configBuilder.withDockerHost(dockerHost)
        }
        val certs: Optional<String> = dockerCertificates()
        certs.ifPresent {
            configBuilder.withDockerCertPath(it)
        }
        registryAuth(configBuilder)
        val config: DockerClientConfig = configBuilder.build()
        return DockerClientImpl.getInstance(config)
            .withDockerCmdExecFactory(factory)
    }
}