package com.github.godfather1103.gradle.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.github.godfather1103.gradle.entity.AuthConfig;
import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.google.common.base.Optional;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.RegistryAuth;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

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
public abstract class AbstractDockerMojo implements Action<DockerClient> {

    protected DockerPluginExtension ext;

    private final NettyDockerCmdExecFactory factory = new NettyDockerCmdExecFactory();

    public AbstractDockerMojo(DockerPluginExtension ext) {
        this.ext = ext;
    }

    public abstract void initExt(DockerPluginExtension ext);

    private final static Logger logger = LoggerFactory.getLogger(AbstractDockerMojo.class);

    public Logger getLog() {
        return logger;
    }

    public Boolean isSkipDocker() {
        return ext.getSkipDocker().getOrElse(false);
    }


    public int getRetryPushTimeout() {
        return ext.getRetryPushTimeout().getOrElse(10000);
    }

    public int getReadTimeout() {
        return ext.getReadTimeout().getOrElse(45000);
    }

    public int getRetryPushCount() {
        return ext.getRetryPushCount().getOrElse(5);
    }

    public boolean isSkipDockerPush() {
        return ext.getSkipDocker().getOrElse(false);
    }

    public void execute() {
        if (isSkipDocker()) {
            getLog().info("Skipping docker goal");
            return;
        }
        // 读取最新的值
        initExt(ext);
        try (DockerClient client = buildDockerClient()) {
            execute(client);
        } catch (Exception e) {
            throw new GradleException("Exception caught", e);
        }
    }

    protected String rawDockerHost() {
        return ext.getDockerHost().getOrNull();
    }

    protected Optional<String> dockerCertificates()
            throws DockerCertificateException {
        String dockerCertPath = ext.getDockerCertPath().getOrNull();
        if (isNotEmpty(dockerCertPath)) {
            return Optional.fromNullable(ext.getDockerCertPath().getOrNull());
        } else {
            return Optional.absent();
        }
    }

    protected AuthConfig makeAuthConfig() {
        // 1、首先从项目的构建脚本中读取
        AuthConfig authConfig = ext.getAuth().getOrNull();
        // 2、没有的话看gradle.properties文件中是否有相关配置
        if (authConfig == null) {
            Object obj = ext.getProject().findProperty("docker.username");
            String username = obj == null ? "" : obj.toString();
            obj = ext.getProject().findProperty("docker.password");
            String password = obj == null ? "" : obj.toString();
            obj = ext.getProject().findProperty("docker.email");
            String email = obj == null ? "" : obj.toString();
            if (isNotEmpty(username) && isNotEmpty(password)) {
                authConfig = new AuthConfig(username, password, email);
            }
        }
        // 3、最后环境看一下环境变量中是否有相关配置
        if (authConfig == null) {
            String username = StringUtils.trimToEmpty(System.getenv("docker.username"));
            String password = StringUtils.trimToEmpty(System.getenv("docker.password"));
            String email = StringUtils.trimToEmpty(System.getenv("docker.email"));
            if (isNotEmpty(username) && isNotEmpty(password)) {
                authConfig = new AuthConfig(username, password, email);
            }
        }
        return authConfig;
    }

    protected RegistryAuth registryAuth() throws GradleException {
        AuthConfig authConfig = makeAuthConfig();
        if (authConfig != null) {
            final RegistryAuth.Builder registryAuthBuilder = RegistryAuth.builder();
            final String username = authConfig.getUsername();
            final String password = authConfig.getPassword();
            final String email = authConfig.getEmail();
            if (isNotEmpty(username)) {
                registryAuthBuilder.username(username);
            }
            if (isNotEmpty(email)) {
                registryAuthBuilder.email(email);
            }
            if (isNotEmpty(password)) {
                registryAuthBuilder.password(password);
            }
            String registryUrl = ext.getRegistryUrl().getOrNull();
            if (isNotEmpty(registryUrl)) {
                registryAuthBuilder.serverAddress(registryUrl);
            }
            return registryAuthBuilder.build();
        } else {
            // settings.xml has no entry for the configured serverId, warn the user
            getLog().warn("No entry found, cannot configure authentication for that registry");
        }
        return null;
    }

    private String serverIdFor(RegistryAuth registryAuth) {
        if (ext.getServerId().getOrNull() != null) {
            return ext.getServerId().getOrNull();
        }
        if (registryAuth.serverAddress() != null) {
            return registryAuth.serverAddress();
        }
        return "index.docker.io";
    }

    protected DockerClient buildDockerClient() throws GradleException {
        final DefaultDockerClientConfig.Builder configBuilder;
        try {
            configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            final String dockerHost = rawDockerHost();
            if (isNotEmpty(dockerHost)) {
                configBuilder.withDockerHost(dockerHost);
            }
            final Optional<String> certs = dockerCertificates();
            if (certs.isPresent()) {
                configBuilder.withDockerCertPath(certs.get());
            }
        } catch (DockerCertificateException ex) {
            throw new GradleException("Cannot build DockerClient due to certificate problem", ex);
        }
        final RegistryAuth registryAuth = registryAuth();
        if (registryAuth != null) {
            configBuilder.withRegistryUsername(registryAuth.username());
            configBuilder.withRegistryPassword(registryAuth.password());
            configBuilder.withRegistryEmail(registryAuth.email());
            configBuilder.withRegistryUrl(serverIdFor(registryAuth));
        }
        DockerClientConfig config = configBuilder.build();
        return DockerClientImpl.getInstance(config)
                .withDockerCmdExecFactory(factory);
    }
}
