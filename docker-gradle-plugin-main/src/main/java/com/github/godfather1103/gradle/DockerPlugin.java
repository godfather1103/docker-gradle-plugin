package com.github.godfather1103.gradle;

import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

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
            e.doLast(a -> System.out.println(123));
        });
    }
}
