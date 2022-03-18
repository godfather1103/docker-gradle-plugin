package com.github.godfather1103.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DockerPlugin implements Plugin<Project> {
    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    @Override
    public void apply(Project target) {
        System.out.println("激活了插件");
    }
}
