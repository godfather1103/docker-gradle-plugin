package com.github.godfather1103.gradle.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.godfather1103.gradle.entity.Git;
import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.spotify.docker.client.DockerClient;
import io.vavr.control.Try;
import org.gradle.api.GradleException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BuildMojo extends AbstractDockerMojo {

    private static final Lock LOCK = new ReentrantLock();

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * Json Object Mapper to encode arguments map
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Directory containing the Dockerfile. If the value is not set, the plugin will generate a
     * Dockerfile using the required baseImage value, plus the optional entryPoint, cmd and maintainer
     * values. If this value is set the plugin will use the Dockerfile in the specified folder.
     */
    private String dockerDirectory;

    /**
     * Flag to skip docker build, making build goal a no-op. This can be useful when docker:build
     * is bound to package goal, and you want to build a jar but not a container. Defaults to false.
     */
    private boolean skipDockerBuild;

    /**
     * Flag to attempt to pull base images even if older images exists locally. Sends the equivalent
     * of `--pull=true` to Docker daemon when building the image.
     */
    private boolean pullOnBuild;

    /**
     * Set to true to pass the `--no-cache` flag to the Docker daemon when building an image.
     */
    private boolean noCache;

    /**
     * Set to false to pass the `--rm` flag to the Docker daemon when building an image.
     */
    private boolean rm;

    /**
     * File path to save image as a tar archive after it is built.
     */
    private String saveImageToTarArchive;

    /**
     * Flag to push image after it is built. Defaults to false.
     */
    private boolean pushImage;

    /**
     * Flag to push image using their tags after it is built. Defaults to false.
     */
    private boolean pushImageTag;

    /**
     * Flag to use force option while tagging. Defaults to false.
     */
    private boolean forceTags;

    /**
     * The maintainer of the image. Ignored if dockerDirectory is set.
     */
    private String maintainer;

    /**
     * The base image to use. Ignored if dockerDirectory is set.
     */
    private String baseImage;

    /**
     * The entry point of the image. Ignored if dockerDirectory is set.
     */
    private String entryPoint;

    /**
     * The volumes for the image
     */
    private String[] volumes;

    /**
     * The labels for the image
     */
    private String[] labels;

    /**
     * The cmd command for the image. Ignored if dockerDirectory is set.
     */
    private String cmd;

    /**
     * The workdir for the image. Ignored if dockerDirectory is set
     */
    private String workdir;

    /**
     * The user for the image. Ignored if dockerDirectory is set
     */
    private String user;

    /**
     * The run commands for the image.
     */
    private List<String> runs;

    private List<String> runList;

    /**
     * Flag to squash all run commands into one layer. Defaults to false.
     */
    private boolean squashRunCommands;

    /**
     * All resources will be copied to this directory before building the image.
     */
    protected String buildDirectory;

    private String profile;

    /**
     * Path to JSON file to write when tagging images.
     * Default is ${project.build.testOutputDirectory}/image_info.json
     */
    protected String tagInfoFile;

    /**
     * If specified as true, a tag will be generated consisting of the first 7 characters of the most
     * recent git commit ID, resulting in something like {@code image:df8e8e6}. If there are any
     * changes not yet committed, the string '.DIRTY' will be appended to the end. Note, if a tag is
     * explicitly specified in the {@code newName} parameter, this flag will be ignored.
     */
    private boolean useGitCommitId;

//    @Parameter(property = "dockerResources")
//    private List<Resource> resources;

    /**
     * Built image will be given this name.
     */
    private String imageName;

    /**
     * Additional tags to tag the image with.
     */
    private List<String> imageTags;

    private String defaultProfile;

    private Map<String, String> env;

    private List<String> exposes;

    private Set<String> exposesSet;

    private Map<String, String> buildArgs;

    /**
     * HEALTHCHECK. It expects a element for 'options' and 'cmd'
     * Added in docker 1.12 (https://docs.docker.com/engine/reference/builder/#/healthcheck).
     */
    private Map<String, String> healthcheck;

    /**
     * Set the networking mode for the RUN instructions during build
     */
    private String network;

    private final Map<String, String> replaceMap = new HashMap<>(0);


    public BuildMojo(DockerPluginExtension ext) {
        super(ext);
        dockerDirectory = ext.getDockerDirectory().getOrNull();
        skipDockerBuild = ext.getSkipDockerBuild().getOrElse(false);
        pullOnBuild = ext.getPullOnBuild().getOrElse(false);
        noCache = ext.getNoCache().getOrElse(false);
        rm = ext.getRm().getOrElse(true);
        saveImageToTarArchive = ext.getSaveImageToTarArchive().getOrNull();
        pushImage = ext.getPushImage().getOrElse(false);
        pushImageTag = ext.getPushImageTag().getOrElse(false);
        forceTags = ext.getForceTags().getOrElse(false);
        maintainer = ext.getDockerMaintainer().getOrNull();
        baseImage = ext.getDockerBaseImage().getOrNull();
        entryPoint = ext.getDockerEntryPoint().getOrNull();
        volumes = ext.getDockerVolumes().getOrElse(new ArrayList<>(0)).toArray(new String[0]);
        labels = ext.getDockerLabels().getOrElse(new ArrayList<>(0)).toArray(new String[0]);
        cmd = ext.getDockerCmd().getOrNull();
        workdir = ext.getWorkdir().getOrNull();
        user = ext.getUser().getOrNull();
        runs = ext.getDockerRuns().getOrNull();
        squashRunCommands = ext.getSquashRunCommands().getOrElse(false);
        buildDirectory = ext.getBuildDirectory();
        profile = ext.getDockerBuildProfile().getOrNull();
        tagInfoFile = ext.getTagInfoFile().getOrNull();
        useGitCommitId = ext.getUseGitCommitId().getOrElse(false);
//        resources = ext
        imageName = ext.getImageName().getOrNull();
        imageTags = ext.getDockerImageTags().getOrElse(new ArrayList<>(0));
        defaultProfile = ext.getDockerDefaultBuildProfile().getOrNull();
        env = ext.getDockerEnv().getOrElse(new HashMap<>(0));
        exposes = ext.getDockerExposes().getOrElse(new ArrayList<>(0));
        buildArgs = ext.getDockerBuildArgs().getOrNull();
        healthcheck = ext.getHealthcheck().getOrNull();
        network = ext.getNetwork().getOrNull();

        ext.getProject().getProperties().forEach((k, v) -> {
            if (v != null) {
                replaceMap.put(k, v.toString());
            }
        });
    }

    private Boolean isSkipDockerBuild() {
        String f = System.getProperty("skipDockerBuild");
        return Strings.isNullOrEmpty(f) ? skipDockerBuild : Boolean.valueOf(f);
    }

    private boolean weShouldSkipDockerBuild() {
        if (isSkipDockerBuild()) {
            getLog().info("Property skipDockerBuild is set");
            return true;
        }
        if (!ext.getProject().getChildProjects().isEmpty()) {
            getLog().info("Project packaging is parent");
            return true;
        }

        if (dockerDirectory != null) {
            final Path path = Paths.get(dockerDirectory, "Dockerfile");
            if (!path.toFile().exists()) {
                getLog().info("No Dockerfile in dockerDirectory");
                return true;
            }
        }
        return false;
    }

    /**
     * Performs this action against the given object.
     *
     * @param dockerClient The object to perform the action on.
     */
    @Override
    public void execute(DockerClient dockerClient) {
        Try.run(() -> {
                    if (weShouldSkipDockerBuild()) {
                        getLog().info("Skipping docker build");
                        return;
                    }
                    exposesSet = Sets.newTreeSet(exposes);
                    if (runs != null) {
                        runList = Lists.newArrayList(runs);
                    }

                    final Git git = new Git();
                    final String commitId = git.isRepository() ? git.getCommitId() : null;

                    if (commitId == null) {
                        final String errorMessage =
                                "Not a git repository, cannot get commit ID. Make sure git repository is initialized.";
                        if (useGitCommitId) {
                            throw new GradleException(errorMessage);
                        } else if (imageName != null && imageName.contains("${gitShortCommitId}")) {
                            throw new GradleException(errorMessage);
                        } else {
                            getLog().debug(errorMessage);
                        }
                    } else {
                        // Put the git commit id in the project properties. Image names may contain
                        // ${gitShortCommitId} in which case we want to fill in the actual value using the
                        // expression evaluator. We will do that once here for image names loaded from the pom,
                        // and again in the loadProfile method when we load values from the profile.
                        replaceMap.put("gitShortCommitId", commitId);
                        if (imageName != null) {
                            imageName = expand(imageName);
                        }
                        if (baseImage != null) {
                            baseImage = expand(baseImage);
                        }
                    }

                }).onFailure(e -> getLog().error("dockerBuild Error", e))
                .getOrElseThrow(e -> new GradleException("dockerBuild Error", e));
    }

    public String expand(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        for (String key : replaceMap.keySet()) {
            value = value.replace("${" + key + "}", replaceMap.get(key));
        }
        return value;
    }

    @Override
    public void execute() {
        try {
            LOCK.lock();
            super.execute();
        } finally {
            LOCK.unlock();
        }
    }
}
