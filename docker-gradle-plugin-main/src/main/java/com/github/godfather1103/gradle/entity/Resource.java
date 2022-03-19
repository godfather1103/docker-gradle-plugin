package com.github.godfather1103.gradle.entity;

import java.io.Serializable;
import java.util.List;

public class Resource implements Serializable, Cloneable {
    private List<String> includes;
    private List<String> excludes;
    private String directory;
    private String targetPath;

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}
