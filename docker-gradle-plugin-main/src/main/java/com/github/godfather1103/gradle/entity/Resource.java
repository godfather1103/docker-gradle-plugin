package com.github.godfather1103.gradle.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2022</p>
 * <p>Company:      https://github.com/godfather1103</p>
 * 资源配置
 *
 * @author 作者: Jack Chu E-mail: chuchuanbao@gmail.com<BR>
 * 创建时间：2022-04-08 21:48
 * @version 1.0
 * @since 1.0
 */
public class Resource implements Serializable, Cloneable {
    private List<String> includes = new ArrayList<>(0);
    private List<String> excludes = new ArrayList<>(0);
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
