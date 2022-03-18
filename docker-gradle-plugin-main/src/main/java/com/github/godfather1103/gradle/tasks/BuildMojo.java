package com.github.godfather1103.gradle.tasks;

import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.spotify.docker.client.DockerClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BuildMojo extends AbstractDockerMojo {

    private static final Lock LOCK = new ReentrantLock();

    private String dockerDirectory;

    public BuildMojo(DockerPluginExtension ext) {
        super(ext);
        dockerDirectory = ext.getDockerDirectory().getOrNull();
    }

    private Boolean isSkipDockerBuild() {
        return Boolean.valueOf(System.getProperty(
                "skipDockerBuild",
                ext.getSkipDockerBuild().get().toString()
        ));
    }

    private boolean weShouldSkipDockerBuild() {
        if (isSkipDockerBuild()) {
            getLog().info("Property skipDockerBuild is set");
            return true;
        }
        if (!ext.getProject().getChildProjects().isEmpty()) {
            getLog().info("Project packaging is parent");
            return true;
        }

        if (dockerDirectory != null) {
            final Path path = Paths.get(dockerDirectory, "Dockerfile");
            if (!path.toFile().exists()) {
                getLog().info("No Dockerfile in dockerDirectory");
                return true;
            }
        }
        return false;
    }

    /**
     * Performs this action against the given object.
     *
     * @param dockerClient The object to perform the action on.
     */
    @Override
    public void execute(DockerClient dockerClient) {

    }

    @Override
    public void execute() {
        try {
            LOCK.lock();
            super.execute();
        } finally {
            LOCK.unlock();
        }
    }
}
