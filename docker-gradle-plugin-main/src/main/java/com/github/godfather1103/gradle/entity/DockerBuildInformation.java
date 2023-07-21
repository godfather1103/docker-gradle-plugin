package com.github.godfather1103.gradle.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.gradle.api.GradleException;
import org.slf4j.Logger;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.google.common.base.Strings.isNullOrEmpty;

public class DockerBuildInformation {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(ORDER_MAP_ENTRIES_BY_KEYS, true)
            .setSerializationInclusion(NON_NULL);

    @JsonProperty("image")
    private final String image;

    @JsonProperty("repo")
    private String repo;

    @JsonProperty("commit")
    private String commit;

    @JsonProperty("digest")
    private String digest;

    public DockerBuildInformation(final String image, final Logger log) {
        this.image = image;
        updateGitInformation(log);
    }

    public DockerBuildInformation setDigest(final String digest) {
        this.digest = digest;
        return this;
    }

    private void updateGitInformation(Logger log) {
        try {
            final Repository repo = new Git().getRepo();
            if (repo != null) {
                this.repo = repo.getConfig().getString("remote", "origin", "url");
                final ObjectId head = repo.resolve("HEAD");
                if (head != null && !isNullOrEmpty(head.getName())) {
                    this.commit = head.getName();
                }
            }
        } catch (IOException e) {
            log.error("Failed to read Git information", e);
        }
    }


    public byte[] toJsonBytes() {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            throw new GradleException("json err", e);
        }
    }

    public String getImage() {
        return image;
    }

    public String getRepo() {
        return repo;
    }

    public String getCommit() {
        return commit;
    }

    public String getDigest() {
        return digest;
    }
}
