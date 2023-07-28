package com.github.godfather1103.gradle.entity

import org.apache.commons.lang3.StringUtils
import org.gradle.api.GradleException

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2023</p>
 * <p>Company:      https://github.com/godfather1103</p>
 *
 * @author  作者: Jack Chu E-mail: chuchuanbao@gmail.com<BR>
 * 创建时间：2023-07-26 23:00
 * @version 1.0
 * @since  1.0
 */
class CompositeImageName private constructor(val name: String, val imageTags: List<String>) {
    companion object {
        @JvmStatic
        fun create(imageName: String, imageTags: List<String>): CompositeImageName {
            val containsTag = containsTag(imageName)
            val name = if (containsTag) StringUtils.substringBeforeLast(imageName, ":") else imageName
            if (StringUtils.isBlank(name)) {
                throw GradleException("imageName not set!")
            }
            val tags: MutableList<String> = ArrayList()
            val tag = if (containsTag) StringUtils.substringAfterLast(imageName, ":") else ""
            if (StringUtils.isNotBlank(tag)) {
                tags.add(tag)
            }
            if (imageTags.isNotEmpty()) {
                tags.addAll(imageTags)
            }
            if (tags.size == 0) {
                throw GradleException("No tag included in imageName and no imageTags set!")
            }
            return CompositeImageName(name, imageTags)
        }

        private fun containsTag(imageName: String): Boolean {
            if (StringUtils.contains(imageName, ":")) {
                if (StringUtils.contains(imageName, "/")) {
                    val registryPart = StringUtils.substringBeforeLast(imageName, "/")
                    val imageNamePart = StringUtils.substring(imageName, registryPart.length + 1)
                    return StringUtils.contains(imageNamePart, ":")
                } else {
                    return true
                }
            }
            return false
        }
    }
}