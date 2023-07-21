package com.github.godfather1103.gradle.entity;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.List;

public class CompositeImageName {

    private final List<String> imageTags;
    private final String name;

    private CompositeImageName(final String name, final List<String> imageTags) {
        this.name = name;
        this.imageTags = imageTags;
    }

    /**
     * An image name can be a plain image name or in the composite format &lt;name&gt;:&lt;tag&gt; and
     * this factory method makes sure that we get the plain image name as well as all the desired tags
     * for an image, including any composite tag.
     *
     * @param imageName Image name.
     * @param imageTags List of image tags.
     * @return {@link CompositeImageName}
     * @throws GradleException 相关异常
     */
    public static CompositeImageName create(final String imageName, final List<String> imageTags)
            throws GradleException {

        final boolean containsTag = containsTag(imageName);

        final String name = containsTag ? StringUtils.substringBeforeLast(imageName, ":") : imageName;
        if (StringUtils.isBlank(name)) {
            throw new GradleException("imageName not set!");
        }

        final List<String> tags = new ArrayList<>();
        final String tag = containsTag ? StringUtils.substringAfterLast(imageName, ":") : "";
        if (StringUtils.isNotBlank(tag)) {
            tags.add(tag);
        }
        if (imageTags != null) {
            tags.addAll(imageTags);
        }
        if (tags.size() == 0) {
            throw new GradleException("No tag included in imageName and no imageTags set!");
        }
        return new CompositeImageName(name, tags);
    }

    public String getName() {
        return name;
    }

    public List<String> getImageTags() {
        return imageTags;
    }

    static boolean containsTag(String imageName) {
        if (StringUtils.contains(imageName, ":")) {
            if (StringUtils.contains(imageName, "/")) {
                final String registryPart = StringUtils.substringBeforeLast(imageName, "/");
                final String imageNamePart = StringUtils.substring(imageName, registryPart.length() + 1);

                return StringUtils.contains(imageNamePart, ":");
            } else {
                return true;
            }
        }

        return false;
    }
}
