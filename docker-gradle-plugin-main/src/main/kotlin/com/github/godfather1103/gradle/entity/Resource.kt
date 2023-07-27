package com.github.godfather1103.gradle.entity

import java.io.Serializable
import java.util.*

/**
 *
 * Title:        Godfather1103's Github
 *
 * Copyright:    Copyright (c) 2022
 *
 * Company:      https://github.com/godfather1103
 * 资源配置
 *
 * @author 作者: Jack Chu E-mail: chuchuanbao@gmail.com<BR></BR>
 * 创建时间：2022-04-08 21:48
 * @version 1.0
 * @since 1.0
 */
class Resource : Serializable, Cloneable {

    private var includes: MutableList<String> = ArrayList(0)

    private var excludes: MutableList<String> = ArrayList(0)

    lateinit var directory: String

    var targetPath: String? = null

    fun getIncludes(): List<String> {
        return includes
    }

    fun setIncludes(includes: MutableList<String>) {
        this.includes = Optional.ofNullable(includes).orElse(ArrayList(0))
    }

    fun addIncludes(include: String): Resource {
        includes.add(include)
        return this
    }

    fun addIncludes(includes: Collection<String>?): Resource {
        this.includes.addAll(includes!!)
        return this
    }

    fun getExcludes(): List<String> {
        return excludes
    }

    fun setExcludes(excludes: MutableList<String>) {
        this.excludes = Optional.ofNullable(excludes).orElse(ArrayList(0))
    }

    fun addExcludes(exclude: String): Resource {
        excludes.add(exclude)
        return this
    }

    fun addExcludes(excludes: Collection<String>?): Resource {
        this.excludes.addAll(excludes!!)
        return this
    }
}
