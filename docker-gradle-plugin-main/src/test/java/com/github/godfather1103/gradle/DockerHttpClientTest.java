package com.github.godfather1103.gradle;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;

public class DockerHttpClientTest {

    @Test
    public void testBuild() throws Exception {
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();
        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectTimeout(30000)
                .readTimeout(45000)
                .build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        HashSet<String> set = Sets.newHashSet("trs-situation-web", "trs-situation-web:1.0");
        BuildImageResultCallback callback = dockerClient.buildImageCmd(new File("src/test/Dockerfile"))
                .withTags(set)
                .exec(new BuildImageResultCallback() {
                    @Override
                    public void onNext(BuildResponseItem item) {
                        super.onNext(item);
                        System.out.println(item.getStream());
                    }
                });
        String id = callback.awaitImageId();
        System.out.println(id);
//        for (String s : set) {
//            dockerClient.pushImageCmd(s).start().awaitCompletion();
//        }
    }
}
