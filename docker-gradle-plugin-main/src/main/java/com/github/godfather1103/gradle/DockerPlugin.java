package com.github.godfather1103.gradle;

import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.github.godfather1103.gradle.tasks.BuildMojo;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import java.util.HashSet;
import java.util.Set;

public class DockerPlugin implements Plugin<Project> {
    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    @Override
    public void apply(Project target) {
        System.out.println("激活了插件");
        DockerPluginExtension docker = target.getExtensions().create("docker", DockerPluginExtension.class, target);
        TaskProvider<Task> dockerBuild = target.getTasks().register("dockerBuild");
        dockerBuild.configure(e -> {
            e.setGroup("docker");
            Set<String> dockerBuildDependsOn = docker.getDockerBuildDependsOn().getOrElse(new HashSet<>(0));
            if (!dockerBuildDependsOn.isEmpty()) {
                e.dependsOn(dockerBuildDependsOn.toArray(new Object[0]));
            }
            BuildMojo buildMojo = new BuildMojo(docker);
            e.doLast(a -> buildMojo.execute());
        });
    }
}
