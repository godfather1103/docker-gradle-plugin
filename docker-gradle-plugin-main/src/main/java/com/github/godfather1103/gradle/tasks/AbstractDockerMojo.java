package com.github.godfather1103.gradle.tasks;

import com.github.godfather1103.gradle.entity.AuthConfig;
import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerCertificatesStore;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.auth.ConfigFileRegistryAuthSupplier;
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier;
import com.spotify.docker.client.auth.MultiRegistryAuthSupplier;
import com.spotify.docker.client.auth.RegistryAuthSupplier;
import com.spotify.docker.client.auth.gcr.ContainerRegistryAuthSupplier;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.RegistryConfigs;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    private String dockerHost;

    private String dockerCertPath;

    private String serverId;

    private String registryUrl;

    /**
     * Number of retries for failing pushes, defaults to 5.
     */
    private int retryPushCount;

    /**
     * Retry timeout for failing pushes, defaults to 10 seconds.
     */
    private int retryPushTimeout;

    /**
     * Flag to skip docker goal, making goal a no-op. This can be useful when docker goal
     * is bound to Maven phase, and you want to skip Docker command. Defaults to false.
     */
    private boolean skipDocker;

    /**
     * Flag to skip docker push, making push goal a no-op. This can be useful when docker:push
     * is bound to deploy goal, and you want to deploy a jar but not a container. Defaults to false.
     */
    private boolean skipDockerPush;

    public AbstractDockerMojo(DockerPluginExtension ext) {
        this.ext = ext;
    }

    public void initExt(DockerPluginExtension ext) {
        this.dockerHost = ext.getDockerHost().getOrNull();
        this.dockerCertPath = ext.getDockerCertPath().getOrNull();
        this.serverId = ext.getServerId().getOrNull();
        this.registryUrl = ext.getRegistryUrl().getOrNull();
        this.retryPushCount = ext.getRetryPushCount().getOrElse(5);
        this.retryPushTimeout = ext.getRetryPushTimeout().getOrElse(10000);
        this.skipDocker = ext.getSkipDocker().getOrElse(false);
        this.skipDockerPush = ext.getSkipDocker().getOrElse(false);
    }

    private final static Logger logger = LoggerFactory.getLogger(AbstractDockerMojo.class);

    public Logger getLog() {
        return logger;
    }

    public Boolean isSkipDocker() {
        return skipDocker;
    }


    public int getRetryPushTimeout() {
        return retryPushTimeout;
    }

    public int getRetryPushCount() {
        return retryPushCount;
    }

    public boolean isSkipDockerPush() {
        return skipDockerPush;
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

    protected DefaultDockerClient.Builder getBuilder() throws DockerCertificateException {
        return DefaultDockerClient.fromEnv()
                .readTimeoutMillis(0);
    }

    protected String rawDockerHost() {
        return ext.getDockerHost().getOrNull();
    }

    protected Optional<DockerCertificatesStore> dockerCertificates()
            throws DockerCertificateException {
        String dockerCertPath = ext.getDockerCertPath().getOrNull();
        if (isNotEmpty(dockerCertPath)) {
            return DockerCertificates.builder()
                    .dockerCertPath(Paths.get(dockerCertPath)).build();
        } else {
            return Optional.absent();
        }
    }

    private RegistryAuthSupplier googleContainerRegistryAuthSupplier() throws GradleException {
        GoogleCredentials credentials = null;

        final String googleCredentialsPath = System.getenv("DOCKER_GOOGLE_CREDENTIALS");
        if (googleCredentialsPath != null) {
            final File file = new File(googleCredentialsPath);
            if (file.exists()) {
                try {
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        credentials = GoogleCredentials.fromStream(inputStream);
                        getLog().info("Using Google credentials from file: " + file.getAbsolutePath());
                    }
                } catch (IOException ex) {
                    throw new GradleException("Cannot load credentials referenced by "
                            + "DOCKER_GOOGLE_CREDENTIALS environment variable", ex);
                }
            }
        }

        // use the ADC last
        if (credentials == null) {
            try {
                credentials = GoogleCredentials.getApplicationDefault();
                getLog().info("Using Google application default credentials");
            } catch (IOException ex) {
                // No GCP default credentials available
                getLog().debug("Failed to load Google application default credentials", ex);
            }
        }

        if (credentials == null) {
            return null;
        }

        return ContainerRegistryAuthSupplier.forCredentials(credentials).build();
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

    private RegistryAuthSupplier authSupplier() throws GradleException {

        final List<RegistryAuthSupplier> suppliers = new ArrayList<>();

        // prioritize the docker config file
        suppliers.add(new ConfigFileRegistryAuthSupplier());

        // then Google Container Registry support
        final RegistryAuthSupplier googleRegistrySupplier = googleContainerRegistryAuthSupplier();
        if (googleRegistrySupplier != null) {
            suppliers.add(googleRegistrySupplier);
        }

        // lastly, use any explicitly configured RegistryAuth as a catch-all
        final RegistryAuth registryAuth = registryAuth();
        if (registryAuth != null) {
            final RegistryConfigs configsForBuild = RegistryConfigs.create(ImmutableMap.of(
                    serverIdFor(registryAuth), registryAuth
            ));
            suppliers.add(new FixedRegistryAuthSupplier(registryAuth, configsForBuild));
        }

        getLog().info("Using authentication suppliers: " +
                Lists.transform(suppliers, new SupplierToClassNameFunction()));

        return new MultiRegistryAuthSupplier(suppliers);
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

        final DefaultDockerClient.Builder builder;
        try {
            builder = getBuilder();
            final String dockerHost = rawDockerHost();
            if (isNotEmpty(dockerHost)) {
                builder.uri(dockerHost);
            }
            final Optional<DockerCertificatesStore> certs = dockerCertificates();
            if (certs.isPresent()) {
                builder.dockerCertificates(certs.get());
            }
        } catch (DockerCertificateException ex) {
            throw new GradleException("Cannot build DockerClient due to certificate problem", ex);
        }
        builder.registryAuthSupplier(authSupplier());
        return builder.build();
    }

    private static class SupplierToClassNameFunction
            implements Function<RegistryAuthSupplier, String> {

        @Override
        @Nonnull
        public String apply(@Nonnull final RegistryAuthSupplier input) {
            return input.getClass().getSimpleName();
        }
    }
}
