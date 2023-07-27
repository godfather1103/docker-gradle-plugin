package com.github.godfather1103.gradle.tasks

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.godfather1103.gradle.entity.DockerBuildInformation
import com.github.godfather1103.gradle.entity.Git
import com.github.godfather1103.gradle.entity.Resource
import com.github.godfather1103.gradle.ext.DockerPluginExtension
import com.github.godfather1103.gradle.utils.Utils.makeOutMsg
import com.github.godfather1103.gradle.utils.Utils.parseImageName
import com.github.godfather1103.gradle.utils.Utils.pushImage
import com.github.godfather1103.gradle.utils.Utils.pushImageTag
import com.github.godfather1103.gradle.utils.Utils.saveImage
import com.github.godfather1103.gradle.utils.Utils.writeImageInfoFile
import com.google.common.base.CharMatcher.WHITESPACE
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.google.common.collect.Sets
import io.vavr.control.Try
import org.apache.commons.lang3.StringUtils
import org.apache.tools.ant.DirectoryScanner
import org.gradle.api.GradleException
import org.gradle.internal.impldep.org.codehaus.plexus.util.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class BuildMojo(ext: DockerPluginExtension) : AbstractDockerMojo(ext) {

    private val LOCK: Lock = ReentrantLock()

    /**
     * The Unix separator character.
     */
    private val UNIX_SEPARATOR = '/'

    /**
     * The Windows separator character.
     */
    private val WINDOWS_SEPARATOR = '\\'

    private var dockerDirectory: String? = null

    private var dockerDirectoryIncludes: Set<String>? = null

    private var dockerDirectoryExcludes: Set<String>? = null

    /**
     * Flag to skip docker build, making build goal a no-op. This can be useful when docker:build
     * is bound to package goal, and you want to build a jar but not a container. Defaults to false.
     */
    private var skipDockerBuild = false

    /**
     * Flag to attempt to pull base images even if older images exists locally. Sends the equivalent
     * of `--pull=true` to Docker daemon when building the image.
     */
    private var pullOnBuild = false

    /**
     * Set to true to pass the `--no-cache` flag to the Docker daemon when building an image.
     */
    private var noCache = false

    /**
     * Set to false to pass the `--rm` flag to the Docker daemon when building an image.
     */
    private var rm = false

    /**
     * File path to save image as a tar archive after it is built.
     */
    private var saveImageToTarArchive: String? = null

    /**
     * Flag to push image after it is built. Defaults to false.
     */
    private var pushImage = false

    /**
     * Flag to push image using their tags after it is built. Defaults to false.
     */
    private var pushImageTag = false

    /**
     * Flag to use force option while tagging. Defaults to false.
     */
    private var forceTags = false

    /**
     * The maintainer of the image. Ignored if dockerDirectory is set.
     */
    private var maintainer: String? = null

    /**
     * The base image to use. Ignored if dockerDirectory is set.
     */
    private var baseImage: String? = null

    /**
     * The entry point of the image. Ignored if dockerDirectory is set.
     */
    private var entryPoint: String? = null

    /**
     * The volumes for the image
     */
    private var volumes: Array<String>? = null

    /**
     * The labels for the image
     */
    private var labels: Array<String>? = null

    /**
     * The cmd command for the image. Ignored if dockerDirectory is set.
     */
    private var cmd: String? = null

    /**
     * The workdir for the image. Ignored if dockerDirectory is set
     */
    private var workdir: String? = null

    /**
     * The user for the image. Ignored if dockerDirectory is set
     */
    private var user: String? = null

    /**
     * The run commands for the image.
     */
    private var runs: List<String>? = null

    private var runList: List<String>? = null

    /**
     * Flag to squash all run commands into one layer. Defaults to false.
     */
    private var squashRunCommands = false

    /**
     * All resources will be copied to this directory before building the image.
     */
    protected var buildDirectory: String? = null

    /**
     * Path to JSON file to write when tagging images.
     * Default is ${project.build.testOutputDirectory}/image_info.json
     */
    protected var tagInfoFile: String? = null

    /**
     * If specified as true, a tag will be generated consisting of the first 7 characters of the most
     * recent git commit ID, resulting in something like `image:df8e8e6`. If there are any
     * changes not yet committed, the string '.DIRTY' will be appended to the end. Note, if a tag is
     * explicitly specified in the `newName` parameter, this flag will be ignored.
     */
    private var useGitCommitId = false

    /**
     * Built image will be given this name.
     */
    private lateinit var imageName: String

    /**
     * Additional tags to tag the image with.
     */
    private var imageTags: List<String> = ArrayList()

    private var env: Map<String, String>? = null

    private lateinit var exposes: List<String>

    private var exposesSet: Set<String>? = null

    private var buildArgs: Map<String, String>? = null

    /**
     * HEALTHCHECK. It expects a element for 'options' and 'cmd'
     * Added in docker 1.12 (https://docs.docker.com/engine/reference/builder/#/healthcheck).
     */
    private var healthcheck: Map<String, String>? = null

    /**
     * Set the networking mode for the RUN instructions during build
     */
    private var network: String? = null

    private var needTagLatest: Boolean = true

    private var quiet: Boolean? = null

    private var resources: MutableList<Resource> = ArrayList(0)

    private var replaceMap: MutableMap<String, String> = HashMap(0)

    /**
     * 根据扩展配置进行初始化<BR>
     * @author  作者: Jack Chu E-mail: chuchuanbao@gmail.com
     * @date 创建时间：2023/7/27 20:10
     * @param ext 扩展配置
     */
    override fun initExt(ext: DockerPluginExtension) {
        dockerDirectory = ext.dockerDirectory.getOrNull()
        dockerDirectoryIncludes = ext.dockerDirectoryIncludes.getOrNull()
        dockerDirectoryExcludes = ext.dockerDirectoryExcludes.getOrNull()
        skipDockerBuild = ext.skipDockerBuild.getOrElse(false)
        pullOnBuild = ext.pullOnBuild.getOrElse(false)
        noCache = ext.noCache.getOrElse(false)
        rm = ext.rm.getOrElse(true)
        saveImageToTarArchive = ext.saveImageToTarArchive.getOrNull()
        pushImage = ext.pushImage.getOrElse(false)
        pushImageTag = ext.pushImageTag.getOrElse(false)
        forceTags = ext.forceTags.getOrElse(false)
        maintainer = ext.dockerMaintainer.getOrNull()
        baseImage = ext.dockerBaseImage.getOrNull()
        entryPoint = ext.dockerEntryPoint.getOrNull()
        volumes = ext.dockerVolumes.getOrElse(ArrayList(0)).toTypedArray<String>()
        labels = ext.dockerLabels.getOrElse(ArrayList(0)).toTypedArray<String>()
        cmd = ext.dockerCmd.getOrNull()
        workdir = ext.workdir.getOrNull()
        user = ext.user.getOrNull()
        runs = ext.dockerRuns.getOrNull()
        squashRunCommands = ext.squashRunCommands.getOrElse(false)
        buildDirectory = ext.buildDirectory
        tagInfoFile = ext.tagInfoFile.getOrNull()
        useGitCommitId = ext.useGitCommitId.getOrElse(false)
        imageName = ext.imageName.get()
        imageTags = ext.dockerImageTags.getOrElse(ArrayList(0))
        env = ext.dockerEnv.getOrElse(HashMap(0))
        exposes = ext.dockerExposes.getOrElse(ArrayList(0))
        buildArgs = ext.dockerBuildArgs.getOrNull()
        healthcheck = ext.healthcheck.getOrNull()
        network = ext.network.getOrNull()
        resources.addAll(ext.resources.getOrElse(ArrayList(0)))
        ext.project.properties.forEach { (k: String, v: Any?) ->
            if (v != null) {
                replaceMap[k] = v.toString()
            }
        }
        needTagLatest = ext.needTagLatest.getOrElse(true)
        quiet = ext.quiet.getOrElse(false)
    }

    private fun isSkipDockerBuild(): Boolean {
        val f = System.getProperty("skipDockerBuild")
        return if (StringUtils.isEmpty(f)) skipDockerBuild else java.lang.Boolean.valueOf(f)
    }

    private fun weShouldSkipDockerBuild(): Boolean {
        if (isSkipDockerBuild()) {
            getLog().info("Property skipDockerBuild is set")
            return true
        }
        if (!ext.project.childProjects.isEmpty()) {
            getLog().info("Project packaging is parent")
            return true
        }
        if (dockerDirectory != null) {
            val path = Paths.get(dockerDirectory, "Dockerfile")
            if (!path.toFile().exists()) {
                getLog().info("No Dockerfile in dockerDirectory")
                return true
            }
        }
        return false
    }

    /**
     * Performs this action against the given object.
     *
     * @param t The object to perform the action on.
     */
    override fun execute(dockerClient: DockerClient) {
        Try.run {
            if (weShouldSkipDockerBuild()) {
                getLog().info("Skipping docker build")
                return@run
            }
            exposesSet = Sets.newTreeSet(exposes)
            if (runs != null) {
                runList = Lists.newArrayList(runs!!)
            }

            val git = Git()
            val commitId = if (git.isRepository()) git.getCommitId() else null

            if (commitId == null) {
                val errorMessage =
                    "Not a git repository, cannot get commit ID. Make sure git repository is initialized."
                if (useGitCommitId) {
                    throw GradleException(errorMessage)
                } else if (imageName.contains("\${gitShortCommitId}")) {
                    throw GradleException(errorMessage)
                } else {
                    getLog().debug(errorMessage)
                }
            } else {
                // Put the git commit id in the project properties. Image names may contain
                // ${gitShortCommitId} in which case we want to fill in the actual value using the
                // expression evaluator. We will do that once here for image names loaded from the pom,
                // and again in the loadProfile method when we load values from the profile.
                replaceMap["gitShortCommitId"] = commitId
                imageName = expand(imageName)!!
                if (baseImage != null) {
                    baseImage = expand(baseImage!!)
                }
            }

            validateParameters()
            val repoTag = parseImageName(imageName)
            val repo = repoTag[0]
            val tag = repoTag[1]

            if (useGitCommitId) {
                if (tag != null) {
                    getLog().warn("Ignoring useGitCommitId flag because tag is explicitly set in image name ")
                } else if (commitId == null) {
                    throw GradleException(
                        "Cannot tag with git commit ID because directory not a git repo"
                    )
                } else {
                    imageName = "$repo:$commitId"
                }
            }
            replaceMap["imageName"] = imageName

            val destination = getDestination()
            if (dockerDirectory == null) {
                val copiedPaths = copyResources(destination)
                createDockerFile(destination, copiedPaths)
            } else {
                val resource = Resource()
                resource.directory = dockerDirectory!!
                resource.addIncludes("build/libs/**")
                    .addIncludes("Docker*")
                    .addIncludes("docker/**")
                    .addExcludes("gradle/**")
                    .addExcludes(".gradle/**")
                    .addExcludes("*gradle*")
                    .addExcludes("src/**")
                if (Objects.nonNull(dockerDirectoryIncludes)) {
                    resource.addIncludes(dockerDirectoryIncludes)
                }
                if (Objects.nonNull(dockerDirectoryExcludes)) {
                    resource.addExcludes(dockerDirectoryExcludes)
                }
                resources.add(resource)
                copyResources(destination)
            }
            buildAndTagImage(dockerClient, destination)
            val buildInfo = DockerBuildInformation(imageName, getLog())
            // Push specific tags specified in pom rather than all images
            if (pushImageTag) {
                pushImageTag(dockerClient, imageName, imageTags, getLog(), isSkipDockerPush())
            }
            if (pushImage) {
                pushImage(
                    dockerClient, imageName, imageTags, getLog(), buildInfo, getRetryPushCount(),
                    getRetryPushTimeout(), isSkipDockerPush()
                )
            }
            if (saveImageToTarArchive != null) {
                saveImage(dockerClient, imageName, Paths.get(saveImageToTarArchive), getLog())
            }
            // Write image info file
            writeImageInfoFile(buildInfo, tagInfoFile!!)
        }.onFailure { getLog().error("dockerBuild Error", it) }
            .getOrElseThrow { e -> GradleException("dockerBuild Error", e) }
    }

    private fun buildAndTagImage(dockerClient: DockerClient, destination: String) {
        val imageId = buildImage(dockerClient, Optional.ofNullable(ext.platform.getOrNull()), destination)
        tagImage(dockerClient, imageId, forceTags)
    }

    @Throws(GradleException::class)
    private fun validateParameters() {
        if (dockerDirectory == null) {
            if (baseImage == null) {
                throw GradleException("Must specify baseImage if dockerDirectory is null")
            }
        } else {
            if (baseImage != null) {
                getLog().warn("Ignoring baseImage because dockerDirectory is set")
            }
            if (maintainer != null) {
                getLog().warn("Ignoring maintainer because dockerDirectory is set")
            }
            if (entryPoint != null) {
                getLog().warn("Ignoring entryPoint because dockerDirectory is set")
            }
            if (cmd != null) {
                getLog().warn("Ignoring cmd because dockerDirectory is set")
            }
            if (runList != null && !runList!!.isEmpty()) {
                getLog().warn("Ignoring run because dockerDirectory is set")
            }
            if (workdir != null) {
                getLog().warn("Ignoring workdir because dockerDirectory is set")
            }
            if (user != null) {
                getLog().warn("Ignoring user because dockerDirectory is set")
            }
        }
    }

    @Throws(GradleException::class, DockerException::class)
    private fun buildImage(docker: DockerClient, platform: Optional<String>, buildDir: String): String {
        getLog().info("Building image $imageName")
        val cmd = buildParams(docker.buildImageCmd(File(Paths.get(buildDir, "Dockerfile").toUri())))
        platform.ifPresent { cmd.withPlatform(it) }
        val callback: BuildImageResultCallback = cmd.exec(object : BuildImageResultCallback() {
            override fun onNext(item: BuildResponseItem) {
                super.onNext(item)
                val msg = makeOutMsg(item)
                println(msg)
            }
        })
        getLog().info("Built $imageName")
        val readTimeout = getReadTimeout()
        val imageId: String = if (readTimeout > 0) {
            callback.awaitImageId(readTimeout.toLong(), TimeUnit.MILLISECONDS)
        } else {
            callback.awaitImageId()
        }
        getLog().info("Built ImageId $imageId")
        return imageId
    }

    private fun buildParams(cmd: BuildImageCmd): BuildImageCmd {
        cmd.withPull(pullOnBuild)
        cmd.withNoCache(noCache)
        cmd.withRemove(rm)
        cmd.withQuiet(quiet)
        buildArgs?.forEach { (t, u) -> cmd.withBuildArg(t, u) }
        if (StringUtils.isNotEmpty(network)) {
            cmd.withNetworkMode(network)
        }
        if (needTagLatest || imageTags.isEmpty()) {
            // 构建latest
            cmd.withTags(setOf(imageName))
        }
        return cmd
    }

    @Throws(DockerException::class, GradleException::class)
    private fun tagImage(docker: DockerClient, imageId: String, forceTags: Boolean) {
        val imageNameWithoutTag = parseImageName(imageName)[0]
        // 构建其他tag
        for (imageTag in imageTags) {
            if (StringUtils.isNotEmpty(imageTag)) {
                getLog().info("Tagging $imageName with $imageTag")
                docker.tagImageCmd(imageId, imageNameWithoutTag, imageTag)
                    .withForce(forceTags)
                    .exec()
            }
        }
    }

    @Throws(IOException::class)
    private fun createDockerFile(directory: String, filesToAdd: List<String>) {
        val commands: MutableList<String> = ArrayList()
        if (baseImage != null) {
            commands.add("FROM $baseImage")
        }
        if (maintainer != null) {
            commands.add("MAINTAINER $maintainer")
        }
        if (env != null) {
            val sortedKeys = Ordering.natural<Comparable<*>>().sortedCopy(
                env!!.keys
            )
            for (key: String in sortedKeys) {
                val value = env!![key]
                commands.add(String.format("ENV %s %s", key, value))
            }
        }
        if (workdir != null) {
            commands.add("WORKDIR $workdir")
        }
        for (file: String in filesToAdd) {
            // The dollar sign in files has to be escaped because docker interprets it as variable
            commands.add(String.format("ADD %s %s", file.replace("\\$".toRegex(), "\\\\\\$"), normalizeDest(file)))
        }
        if (runList != null && !runList!!.isEmpty()) {
            if (squashRunCommands) {
                commands.add("RUN " + Joiner.on(" &&\\\n\t").join(runList!!))
            } else {
                for (run: String in runList!!) {
                    commands.add("RUN $run")
                }
            }
        }
        if (healthcheck != null && healthcheck!!.containsKey("cmd")) {
            val healthcheckBuffer = StringBuffer("HEALTHCHECK ")
            if (healthcheck!!.containsKey("options")) {
                healthcheckBuffer.append(healthcheck!!["options"])
                healthcheckBuffer.append(" ")
            }
            healthcheckBuffer.append("CMD ")
            healthcheckBuffer.append(healthcheck!!["cmd"])
            commands.add(healthcheckBuffer.toString())
        }
        if (exposesSet!!.isNotEmpty()) {
            // The values will be sorted with no duplicated since exposesSet is a TreeSet
            commands.add("EXPOSE " + Joiner.on(" ").join(exposesSet!!))
        }
        if (user != null) {
            commands.add("USER $user")
        }
        if (entryPoint != null) {
            commands.add("ENTRYPOINT $entryPoint")
        }
        if (cmd != null) {
            if (entryPoint != null) {
                // CMD needs to be a list of arguments if ENTRYPOINT is set.
                if (cmd!!.startsWith("[") && cmd!!.endsWith("]")) {
                    // cmd seems to be an argument list, so we're good
                    commands.add("CMD $cmd")
                } else {
                    // cmd does not seem to be an argument list, so try to generate one.
                    val args: List<String> = ImmutableList.copyOf(
                        Splitter.on(WHITESPACE).omitEmptyStrings().split(cmd!!)
                    )
                    val cmdBuilder = StringBuilder("[")
                    for (arg: String in args) {
                        cmdBuilder.append('"').append(arg).append('"')
                    }
                    cmdBuilder.append(']')
                    val cmdString = cmdBuilder.toString()
                    commands.add("CMD $cmdString")
                    getLog().warn(
                        "Entrypoint provided but cmd is not an explicit list. Attempting to " +
                                "generate CMD string in the form of an argument list."
                    )
                    getLog().warn("CMD $cmdString")
                }
            } else {
                // no ENTRYPOINT set so use cmd verbatim
                commands.add("CMD $cmd")
            }
        }

        // Add VOLUME's to dockerfile
        if (volumes != null) {
            for (volume: String in volumes!!) {
                commands.add("VOLUME $volume")
            }
        }

        // Add LABEL's to dockerfile
        if (labels != null) {
            for (label: String in labels!!) {
                commands.add("LABEL $label")
            }
        }
        getLog().debug(
            "Writing Dockerfile:" + System.lineSeparator() +
                    Joiner.on(System.lineSeparator()).join(commands)
        )

        // this will overwrite an existing file
        Files.createDirectories(Paths.get(directory))
        Files.write(Paths.get(directory, "Dockerfile"), commands, StandardCharsets.UTF_8)
    }


    private fun normalizeDest(filePath: String): String {
        val file = File(filePath)
        return if (File(getDestination(), filePath).isFile) {
            if (file.parent != null) {
                // remove file part of path
                separatorsToUnix(file.parent) + "/"
            } else {
                // working with a simple "ADD file.txt"
                "."
            }
        } else {
            separatorsToUnix(file.path)
        }
    }

    @Throws(IOException::class)
    private fun copyResources(destination: String): List<String> {
        val allCopiedPaths: MutableList<String> = ArrayList()
        for (resource in resources) {
            val source = File(resource.directory)
            val includes = resource.getIncludes()
            val excludes = resource.getExcludes()
            val scanner = DirectoryScanner()
            scanner.basedir = source
            // must pass null if includes/excludes is empty to get default filters.
            // passing zero length array forces it to have no filters at all.
            scanner.setIncludes(if (includes.isEmpty()) null else includes.toTypedArray<String>())
            scanner.setExcludes(if (excludes.isEmpty()) null else excludes.toTypedArray<String>())
            scanner.scan()
            val includedFiles = scanner.includedFiles
            if (includedFiles.isEmpty()) {
                getLog().info("No resources will be copied, no files match specified patterns")
            }
            val copiedPaths: MutableList<String> = ArrayList()
            val copyWholeDir = includes.isEmpty() && excludes.isEmpty() && resource.targetPath != null

            // file location relative to docker directory, used later to generate Dockerfile
            val targetPath = if (resource.targetPath == null) "" else resource.targetPath!!
            if (copyWholeDir) {
                val destPath = Paths.get(destination, targetPath)
                getLog().info(String.format("Copying dir %s -> %s", source, destPath))
                Files.createDirectories(destPath)
                FileUtils.copyDirectoryStructure(source, destPath.toFile())
                copiedPaths.add(separatorsToUnix(targetPath))
            } else {
                for (included in includedFiles) {
                    val sourcePath = Paths.get(resource.directory).resolve(included)
                    val destPath = Paths.get(destination, targetPath).resolve(included)
                    getLog().info(String.format("Copying %s -> %s", sourcePath, destPath))
                    Files.createDirectories(destPath.parent)
                    Files.copy(
                        sourcePath,
                        destPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    copiedPaths.add(separatorsToUnix(Paths.get(targetPath).resolve(included).toString()))
                }
            }
            copiedPaths.sort()
            allCopiedPaths.addAll(copiedPaths)
        }
        return allCopiedPaths
    }

    private fun separatorsToUnix(path: String): String {
        return if (path.indexOf(WINDOWS_SEPARATOR) == -1) {
            path
        } else path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR)
    }

    private fun getDestination(): String {
        return Paths.get(buildDirectory, "docker").toString()
    }

    fun expand(value: String): String? {
        if (value.isEmpty()) {
            return value
        }
        var tmp = value
        for (key in replaceMap.keys) {
            tmp = tmp.replace("\${$key}", replaceMap[key]!!)
        }
        return tmp
    }

    override fun execute() {
        LOCK.lock()
        try {
            super.execute()
        } finally {
            LOCK.unlock()
        }
    }
}