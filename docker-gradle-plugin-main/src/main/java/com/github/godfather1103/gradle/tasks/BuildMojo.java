package com.github.godfather1103.gradle.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.godfather1103.gradle.entity.DockerBuildInformation;
import com.github.godfather1103.gradle.entity.Git;
import com.github.godfather1103.gradle.entity.Resource;
import com.github.godfather1103.gradle.ext.DockerPluginExtension;
import com.github.godfather1103.gradle.util.Utils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.spotify.docker.client.AnsiProgressHandler;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import io.vavr.control.Try;
import org.apache.tools.ant.DirectoryScanner;
import org.gradle.api.GradleException;
import org.gradle.internal.impldep.org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.godfather1103.gradle.util.Utils.parseImageName;
import static com.github.godfather1103.gradle.util.Utils.writeImageInfoFile;
import static com.google.common.base.CharMatcher.WHITESPACE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;

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

    private List<Resource> resources;

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
        imageName = ext.getImageName().getOrNull();
        imageTags = ext.getDockerImageTags().getOrElse(new ArrayList<>(0));
        defaultProfile = ext.getDockerDefaultBuildProfile().getOrNull();
        env = ext.getDockerEnv().getOrElse(new HashMap<>(0));
        exposes = ext.getDockerExposes().getOrElse(new ArrayList<>(0));
        buildArgs = ext.getDockerBuildArgs().getOrNull();
        healthcheck = ext.getHealthcheck().getOrNull();
        network = ext.getNetwork().getOrNull();
        this.resources = new ArrayList<>(0);
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

                    validateParameters();

                    final String[] repoTag = parseImageName(imageName);
                    final String repo = repoTag[0];
                    final String tag = repoTag[1];

                    if (useGitCommitId) {
                        if (tag != null) {
                            getLog().warn("Ignoring useGitCommitId flag because tag is explicitly set in image name ");
                        } else if (commitId == null) {
                            throw new GradleException(
                                    "Cannot tag with git commit ID because directory not a git repo");
                        } else {
                            imageName = repo + ":" + commitId;
                        }
                    }
                    replaceMap.put("imageName", imageName);

                    final String destination = getDestination();
                    if (dockerDirectory == null) {
                        final List<String> copiedPaths = copyResources(destination);
                        createDockerFile(destination, copiedPaths);
                    } else {
                        final Resource resource = new Resource();
                        resource.setDirectory(dockerDirectory);
                        resource.setExcludes(new ArrayList<>(3) {{
                            add("gradle/**");
                            add(".gradle/**");
                            add("*gradle*");
                            add("src/**");
                        }});
                        resources.add(resource);
                        copyResources(destination);
                    }

                    buildImage(dockerClient, destination, buildParams());
                    tagImage(dockerClient, forceTags);

                    final DockerBuildInformation buildInfo = new DockerBuildInformation(imageName, getLog());

                    // Push specific tags specified in pom rather than all images
                    if (pushImageTag) {
                        Utils.pushImageTag(dockerClient, imageName, imageTags, getLog(), isSkipDockerPush());
                    }

                    if (pushImage) {
                        Utils.pushImage(dockerClient, imageName, imageTags, getLog(), buildInfo, getRetryPushCount(),
                                getRetryPushTimeout(), isSkipDockerPush());
                    }

                    if (saveImageToTarArchive != null) {
                        Utils.saveImage(dockerClient, imageName, Paths.get(saveImageToTarArchive), getLog());
                    }

                    // Write image info file
                    writeImageInfoFile(buildInfo, tagInfoFile);
                }).onFailure(e -> getLog().error("dockerBuild Error", e))
                .getOrElseThrow(e -> new GradleException("dockerBuild Error", e));
    }

    private void validateParameters() throws GradleException {
        if (dockerDirectory == null) {
            if (baseImage == null) {
                throw new GradleException("Must specify baseImage if dockerDirectory is null");
            }
        } else {
            if (baseImage != null) {
                getLog().warn("Ignoring baseImage because dockerDirectory is set");
            }
            if (maintainer != null) {
                getLog().warn("Ignoring maintainer because dockerDirectory is set");
            }
            if (entryPoint != null) {
                getLog().warn("Ignoring entryPoint because dockerDirectory is set");
            }
            if (cmd != null) {
                getLog().warn("Ignoring cmd because dockerDirectory is set");
            }
            if (runList != null && !runList.isEmpty()) {
                getLog().warn("Ignoring run because dockerDirectory is set");
            }
            if (workdir != null) {
                getLog().warn("Ignoring workdir because dockerDirectory is set");
            }
            if (user != null) {
                getLog().warn("Ignoring user because dockerDirectory is set");
            }
        }
    }

    private void buildImage(final DockerClient docker, final String buildDir,
                            final DockerClient.BuildParam... buildParams)
            throws GradleException, DockerException, IOException, InterruptedException {
        getLog().info("Building image " + imageName);
        docker.build(Paths.get(buildDir), imageName, new AnsiProgressHandler(), buildParams);
        getLog().info("Built " + imageName);
    }

    private void tagImage(final DockerClient docker, boolean forceTags)
            throws DockerException, InterruptedException, GradleException {
        final String imageNameWithoutTag = parseImageName(imageName)[0];
        for (final String imageTag : imageTags) {
            if (!isNullOrEmpty(imageTag)) {
                getLog().info("Tagging " + imageName + " with " + imageTag);
                docker.tag(imageName, imageNameWithoutTag + ":" + imageTag, forceTags);
            }
        }
    }

    private void createDockerFile(final String directory, final List<String> filesToAdd)
            throws IOException {

        final List<String> commands = newArrayList();
        if (baseImage != null) {
            commands.add("FROM " + baseImage);
        }
        if (maintainer != null) {
            commands.add("MAINTAINER " + maintainer);
        }

        if (env != null) {
            final List<String> sortedKeys = Ordering.natural().sortedCopy(env.keySet());
            for (final String key : sortedKeys) {
                final String value = env.get(key);
                commands.add(String.format("ENV %s %s", key, value));
            }
        }

        if (workdir != null) {
            commands.add("WORKDIR " + workdir);
        }

        for (final String file : filesToAdd) {
            // The dollar sign in files has to be escaped because docker interprets it as variable
            commands.add(
                    String.format("ADD %s %s", file.replaceAll("\\$", "\\\\\\$"), normalizeDest(file)));
        }

        if (runList != null && !runList.isEmpty()) {
            if (squashRunCommands) {
                commands.add("RUN " + Joiner.on(" &&\\\n\t").join(runList));
            } else {
                for (final String run : runList) {
                    commands.add("RUN " + run);
                }
            }
        }

        if (healthcheck != null && healthcheck.containsKey("cmd")) {
            final StringBuffer healthcheckBuffer = new StringBuffer("HEALTHCHECK ");
            if (healthcheck.containsKey("options")) {
                healthcheckBuffer.append(healthcheck.get("options"));
                healthcheckBuffer.append(" ");
            }
            healthcheckBuffer.append("CMD ");
            healthcheckBuffer.append(healthcheck.get("cmd"));
            commands.add(healthcheckBuffer.toString());
        }

        if (exposesSet.size() > 0) {
            // The values will be sorted with no duplicated since exposesSet is a TreeSet
            commands.add("EXPOSE " + Joiner.on(" ").join(exposesSet));
        }

        if (user != null) {
            commands.add("USER " + user);
        }

        if (entryPoint != null) {
            commands.add("ENTRYPOINT " + entryPoint);
        }
        if (cmd != null) {
            // TODO(dano): we actually need to check whether the base image has an entrypoint
            if (entryPoint != null) {
                // CMD needs to be a list of arguments if ENTRYPOINT is set.
                if (cmd.startsWith("[") && cmd.endsWith("]")) {
                    // cmd seems to be an argument list, so we're good
                    commands.add("CMD " + cmd);
                } else {
                    // cmd does not seem to be an argument list, so try to generate one.
                    final List<String> args = ImmutableList.copyOf(
                            Splitter.on(WHITESPACE).omitEmptyStrings().split(cmd));
                    final StringBuilder cmdBuilder = new StringBuilder("[");
                    for (final String arg : args) {
                        cmdBuilder.append('"').append(arg).append('"');
                    }
                    cmdBuilder.append(']');
                    final String cmdString = cmdBuilder.toString();
                    commands.add("CMD " + cmdString);
                    getLog().warn("Entrypoint provided but cmd is not an explicit list. Attempting to " +
                            "generate CMD string in the form of an argument list.");
                    getLog().warn("CMD " + cmdString);
                }
            } else {
                // no ENTRYPOINT set so use cmd verbatim
                commands.add("CMD " + cmd);
            }
        }

        // Add VOLUME's to dockerfile
        if (volumes != null) {
            for (final String volume : volumes) {
                commands.add("VOLUME " + volume);
            }
        }

        // Add LABEL's to dockerfile
        if (labels != null) {
            for (final String label : labels) {
                commands.add("LABEL " + label);
            }
        }

        getLog().debug("Writing Dockerfile:" + System.lineSeparator() +
                Joiner.on(System.lineSeparator()).join(commands));

        // this will overwrite an existing file
        Files.createDirectories(Paths.get(directory));
        Files.write(Paths.get(directory, "Dockerfile"), commands, UTF_8);
    }

    private String normalizeDest(final String filePath) {
        // if the path is a file (i.e. not a directory), remove the last part of the path so that we
        // end up with:
        //   ADD foo/bar.txt foo/
        // instead of
        //   ADD foo/bar.txt foo/bar.txt
        // This is to prevent issues when adding tar.gz or other archives where Docker will
        // automatically expand the archive into the "dest", so
        //  ADD foo/x.tar.gz foo/x.tar.gz
        // results in x.tar.gz being expanded *under* the path foo/x.tar.gz/stuff...
        final File file = new File(filePath);

        final String dest;
        // need to know the path relative to destination to test if it is a file or directory,
        // but only remove the last part of the path if there is a parent (i.e. don't remove a
        // parent path segment from "file.txt")
        if (new File(getDestination(), filePath).isFile()) {
            if (file.getParent() != null) {
                // remove file part of path
                dest = separatorsToUnix(file.getParent()) + "/";
            } else {
                // working with a simple "ADD file.txt"
                dest = ".";
            }
        } else {
            dest = separatorsToUnix(file.getPath());
        }

        return dest;
    }

    private List<String> copyResources(String destination) throws IOException {
        final List<String> allCopiedPaths = newArrayList();
        for (final Resource resource : resources) {
            final File source = new File(resource.getDirectory());
            final List<String> includes = resource.getIncludes();
            final List<String> excludes = resource.getExcludes();
            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(source);
            // must pass null if includes/excludes is empty to get default filters.
            // passing zero length array forces it to have no filters at all.
            scanner.setIncludes(includes.isEmpty() ? null
                    : includes.toArray(new String[includes.size()]));
            scanner.setExcludes(excludes.isEmpty() ? null
                    : excludes.toArray(new String[excludes.size()]));
            scanner.scan();

            final String[] includedFiles = scanner.getIncludedFiles();
            if (includedFiles.length == 0) {
                getLog().info("No resources will be copied, no files match specified patterns");
            }

            final List<String> copiedPaths = newArrayList();

            final boolean copyWholeDir = includes.isEmpty() && excludes.isEmpty() &&
                    resource.getTargetPath() != null;

            // file location relative to docker directory, used later to generate Dockerfile
            final String targetPath = resource.getTargetPath() == null ? "" : resource.getTargetPath();

            if (copyWholeDir) {
                final Path destPath = Paths.get(destination, targetPath);
                getLog().info(String.format("Copying dir %s -> %s", source, destPath));

                Files.createDirectories(destPath);
                FileUtils.copyDirectoryStructure(source, destPath.toFile());
                copiedPaths.add(separatorsToUnix(targetPath));
            } else {
                for (final String included : includedFiles) {
                    final Path sourcePath = Paths.get(resource.getDirectory()).resolve(included);
                    final Path destPath = Paths.get(destination, targetPath).resolve(included);
                    getLog().info(String.format("Copying %s -> %s", sourcePath, destPath));
                    // ensure all directories exist because copy operation will fail if they don't
                    Files.createDirectories(destPath.getParent());
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);

                    copiedPaths.add(separatorsToUnix(Paths.get(targetPath).resolve(included).toString()));
                }
            }

            // The list of included files returned from DirectoryScanner can be in a different order
            // each time. This causes the ADD statements in the generated Dockerfile to appear in a
            // different order. We want to avoid this so each run of the plugin always generates the same
            // Dockerfile, which also makes testing easier. Sort the list of paths for each resource
            // before adding it to the allCopiedPaths list. This way we follow the ordering of the
            // resources in the pom, while making sure all the paths of each resource are always in the
            // same order.
            Collections.sort(copiedPaths);
            allCopiedPaths.addAll(copiedPaths);
        }
        return allCopiedPaths;
    }

    public static String separatorsToUnix(final String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    private String getDestination() {
        return Paths.get(buildDirectory, "docker").toString();
    }

    private String get(final String override, final Config config, final String path)
            throws GradleException {
        if (override != null) {
            return override;
        }
        try {
            return expand(config.getString(path));
        } catch (ConfigException.Missing e) {
            return null;
        }
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
        LOCK.lock();
        try {
            super.execute();
        } finally {
            LOCK.unlock();
        }
    }

    private DockerClient.BuildParam[] buildParams()
            throws UnsupportedEncodingException, JsonProcessingException {
        final List<DockerClient.BuildParam> buildParams = Lists.newArrayList();
        if (pullOnBuild) {
            buildParams.add(DockerClient.BuildParam.pullNewerImage());
        }
        if (noCache) {
            buildParams.add(DockerClient.BuildParam.noCache());
        }
        if (!rm) {
            buildParams.add(DockerClient.BuildParam.rm(false));
        }
        if (!buildArgs.isEmpty()) {
            buildParams.add(DockerClient.BuildParam.create("buildargs",
                    URLEncoder.encode(OBJECT_MAPPER.writeValueAsString(buildArgs), "UTF-8")));
        }
        if (!isNullOrEmpty(network)) {
            buildParams.add(DockerClient.BuildParam.create("networkmode", network));
        }
        return buildParams.toArray(new DockerClient.BuildParam[buildParams.size()]);
    }
}
