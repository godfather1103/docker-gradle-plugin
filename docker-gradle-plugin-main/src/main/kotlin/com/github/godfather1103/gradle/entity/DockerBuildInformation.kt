package com.github.godfather1103.gradle.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.base.Strings
import org.gradle.api.GradleException
import org.slf4j.Logger
import java.io.IOException

class DockerBuildInformation {

    private val OBJECT_MAPPER = ObjectMapper()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    @JsonProperty("repo")
    private var image: String

    @JsonProperty("repo")
    private lateinit var repo: String

    @JsonProperty("commit")
    private lateinit var commit: String

    @JsonProperty("digest")
    private lateinit var digest: String

    constructor(image: String, log: Logger) {
        this.image = image
        updateGitInformation(log)
    }

    fun setDigest(digest: String): DockerBuildInformation {
        this.digest = digest
        return this
    }

    fun toJsonBytes(): ByteArray {
        return try {
            OBJECT_MAPPER.writeValueAsBytes(this)
        } catch (e: JsonProcessingException) {
            throw GradleException("json err", e)
        }
    }

    private fun updateGitInformation(log: Logger) {
        try {
            val repo = Git().repo
            if (repo != null) {
                this.repo = repo.config.getString("remote", "origin", "url")
                val head = repo.resolve("HEAD")
                if (head != null && !Strings.isNullOrEmpty(head.name)) {
                    commit = head.name
                }
            }
        } catch (e: IOException) {
            log.error("Failed to read Git information", e)
        }
    }
}