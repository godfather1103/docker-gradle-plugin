package com.github.godfather1103.gradle.utils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.api.model.ResponseItem
import com.github.godfather1103.gradle.entity.CompositeImageName
import com.github.godfather1103.gradle.entity.DockerBuildInformation
import com.google.common.base.Strings
import org.apache.commons.lang3.StringUtils
import org.gradle.api.GradleException
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

object Utils {

    private const val PUSH_FAIL_WARN_TEMPLATE = "Failed to push %s, retrying in %d seconds (%d/%d)."

    @JvmStatic
    fun makeOutMsg(obj: ResponseItem): String {
        val msg = StringBuilder()
        if (StringUtils.isNotEmpty(obj.stream)) {
            msg.append(obj.stream)
        }
        if (StringUtils.isNotEmpty(obj.id)) {
            msg.append(obj.id).append(": ")
        }
        if (StringUtils.isNotEmpty(obj.status)) {
            msg.append(obj.status).append(" ")
        }
        if (StringUtils.isNotEmpty(obj.progress)) {
            msg.append(obj.progress)
        }
        return msg.toString()
    }

    @JvmStatic
    @Throws(GradleException::class)
    fun parseImageName(imageName: String): Array<String?> {
        if (Strings.isNullOrEmpty(imageName)) {
            throw GradleException(
                "You must specify an \"imageName\" in your docker-maven-client's plugin configuration"
            )
        }
        val lastSlashIndex = imageName.lastIndexOf('/')
        val lastColonIndex = imageName.lastIndexOf(':')

        // assume name doesn't contain tag by default
        var repo = imageName
        var tag: String? = null

        // the name contains a tag if lastColonIndex > lastSlashIndex
        if (lastColonIndex > lastSlashIndex) {
            repo = imageName.substring(0, lastColonIndex)
            tag = imageName.substring(lastColonIndex + 1)
            // handle case where tag is empty string (e.g. 'repo:')
            if (tag.isEmpty()) {
                tag = null
            }
        }
        return arrayOf(repo, tag)
    }

    @JvmStatic
    @Throws(GradleException::class, InterruptedException::class)
    fun pushImage(
        docker: DockerClient,
        imageName: String,
        imageTags: List<String>,
        log: Logger,
        buildInfo: DockerBuildInformation,
        retryPushCount: Int,
        retryPushTimeout: Int,
        skipPush: Boolean
    ) {
        if (skipPush) {
            log.info("Skipping docker push")
            return
        }
        var attempt = 0
        do {
            try {
                log.info("Pushing $imageName")
                docker.pushImageCmd(imageName).exec(object : ResultCallback.Adapter<PushResponseItem>() {
                    override fun onNext(item: PushResponseItem) {
                        super.onNext(item)
                        val msg = makeOutMsg(item)
                        println(msg)
                        val aux = Optional.ofNullable(item.aux)
                        if (aux.isPresent) {
                            val imageNameWithoutTag = parseImageName(imageName)[0]
                            buildInfo.setDigest(imageNameWithoutTag + "@" + aux.get().digest)
                        }
                    }
                }).awaitCompletion()
                val imageNameNoTag = getImageNameWithNoTag(imageName)
                for (imageTag in imageTags) {
                    val imageNameAndTag = "$imageNameNoTag:$imageTag"
                    log.info("Pushing $imageNameAndTag")
                    docker.pushImageCmd(imageNameAndTag)
                        .exec(object : ResultCallback.Adapter<PushResponseItem>() {
                            override fun onNext(item: PushResponseItem) {
                                super.onNext(item)
                                val msg = makeOutMsg(item)
                                println(msg)
                            }
                        }).awaitCompletion()
                }
                // A concurrent push raises a generic DockerException and not
                // the more logical ImagePushFailedException. Hence the rather
                // wide catch clause.
            } catch (e: Exception) {
                if (attempt < retryPushCount) {
                    log.warn(
                        String.format(
                            PUSH_FAIL_WARN_TEMPLATE, imageName, retryPushTimeout / 1000,
                            attempt + 1, retryPushCount
                        )
                    )
                    Thread.sleep(retryPushTimeout.toLong())
                    continue
                } else {
                    throw e
                }
            }
            break
        } while (attempt++ <= retryPushCount)
    }

    private fun getImageNameWithNoTag(imageName: String): String {
        val tagSeparatorIndex = imageName.lastIndexOf(':')
        if (tagSeparatorIndex >= 0) {
            return imageName.substring(0, tagSeparatorIndex)
        }
        return imageName
    }

    @JvmStatic
    @Throws(GradleException::class, InterruptedException::class)
    fun pushImageTag(docker: DockerClient, imageName: String, imageTags: List<String>, log: Logger, skipPush: Boolean) {
        if (skipPush) {
            log.info("Skipping docker push")
            return
        }
        // tags should not be empty if you have specified the option to push tags
        if (imageTags.isEmpty()) {
            throw GradleException(
                "You have used option \"pushImageTag\" but have"
                        + " not specified an \"imageTag\" in your"
                        + " docker-maven-client's plugin configuration"
            )
        }
        val compositeImageName = CompositeImageName.create(imageName, imageTags)
        for (imageTag: String in compositeImageName.imageTags) {
            val imageNameWithTag: String = compositeImageName.name + ":" + imageTag
            log.info("Pushing $imageNameWithTag")
            docker.pushImageCmd(imageNameWithTag).exec(object : ResultCallback.Adapter<PushResponseItem>() {
                override fun onNext(item: PushResponseItem) {
                    super.onNext(item)
                    val msg = makeOutMsg(item)
                    println(msg)
                }
            }).awaitCompletion()
        }
    }

    @JvmStatic
    @Throws(DockerException::class, IOException::class)
    fun saveImage(docker: DockerClient, imageName: String, tarArchivePath: Path, log: Logger) {
        log.info(
            String.format(
                "Save docker image %s to %s.",
                imageName, tarArchivePath.toAbsolutePath()
            )
        )
        val input = docker.saveImageCmd(imageName).exec()
        Files.copy(input, tarArchivePath, StandardCopyOption.REPLACE_EXISTING)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeImageInfoFile(buildInfo: DockerBuildInformation, tagInfoFile: String) {
        val imageInfoPath = Paths.get(tagInfoFile)
        if (imageInfoPath.parent != null) {
            Files.createDirectories(imageInfoPath.parent)
        }
        Files.write(imageInfoPath, buildInfo.toJsonBytes())
    }
}