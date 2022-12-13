package com.github.godfather1103.gradle.util;

import com.github.godfather1103.gradle.entity.CompositeImageName;
import com.github.godfather1103.gradle.entity.DockerBuildInformation;
import com.spotify.docker.client.AnsiProgressHandler;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;
import org.gradle.api.GradleException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Thread.sleep;

public class Utils {

    public static final String PUSH_FAIL_WARN_TEMPLATE = "Failed to push %s,"
            + " retrying in %d seconds (%d/%d).";

    public static String[] parseImageName(String imageName) throws GradleException {
        if (isNullOrEmpty(imageName)) {
            throw new GradleException("You must specify an \"imageName\" in your "
                    + "docker-maven-client's plugin configuration");
        }
        final int lastSlashIndex = imageName.lastIndexOf('/');
        final int lastColonIndex = imageName.lastIndexOf(':');

        // assume name doesn't contain tag by default
        String repo = imageName;
        String tag = null;

        // the name contains a tag if lastColonIndex > lastSlashIndex
        if (lastColonIndex > lastSlashIndex) {
            repo = imageName.substring(0, lastColonIndex);
            tag = imageName.substring(lastColonIndex + 1);
            // handle case where tag is empty string (e.g. 'repo:')
            if (tag.isEmpty()) {
                tag = null;
            }
        }

        return new String[]{repo, tag};
    }

    public static void pushImage(final DockerClient docker, final String imageName,
                                 final List<String> imageTags, final Logger log,
                                 final DockerBuildInformation buildInfo,
                                 final int retryPushCount, final int retryPushTimeout,
                                 final boolean skipPush)
            throws GradleException, DockerException, InterruptedException {

        if (skipPush) {
            log.info("Skipping docker push");
            return;
        }

        int attempt = 0;
        do {
            final AnsiProgressHandler ansiProgressHandler = new AnsiProgressHandler();
            final DigestExtractingProgressHandler handler = new DigestExtractingProgressHandler(
                    ansiProgressHandler);

            try {
                log.info("Pushing " + imageName);
                docker.push(imageName, handler);

                if (imageTags != null) {
                    final String imageNameNoTag = getImageNameWithNoTag(imageName);
                    for (final String imageTag : imageTags) {
                        final String imageNameAndTag = imageNameNoTag + ":" + imageTag;
                        log.info("Pushing " + imageNameAndTag);
                        docker.push(imageNameAndTag, new AnsiProgressHandler());
                    }
                }

                // A concurrent push raises a generic DockerException and not
                // the more logical ImagePushFailedException. Hence the rather
                // wide catch clause.
            } catch (DockerException e) {
                if (attempt < retryPushCount) {
                    log.warn(String.format(PUSH_FAIL_WARN_TEMPLATE, imageName, retryPushTimeout / 1000,
                            attempt + 1, retryPushCount));
                    sleep(retryPushTimeout);
                    continue;
                } else {
                    throw e;
                }
            }
            if (buildInfo != null) {
                final String imageNameWithoutTag = parseImageName(imageName)[0];
                buildInfo.setDigest(imageNameWithoutTag + "@" + handler.digest());
            }
            break;
        } while (attempt++ <= retryPushCount);
    }

    private static String getImageNameWithNoTag(String imageName) {
        final int tagSeparatorIndex = imageName.lastIndexOf(':');
        if (tagSeparatorIndex >= 0) {
            imageName = imageName.substring(0, tagSeparatorIndex);
        }
        return imageName;
    }

    // push just the tags listed in the pom rather than all images using imageName
    public static void pushImageTag(DockerClient docker, String imageName,
                                    List<String> imageTags, Logger log, boolean skipPush)
            throws GradleException, DockerException, IOException, InterruptedException {

        if (skipPush) {
            log.info("Skipping docker push");
            return;
        }
        // tags should not be empty if you have specified the option to push tags
        if (imageTags.isEmpty()) {
            throw new GradleException("You have used option \"pushImageTag\" but have"
                    + " not specified an \"imageTag\" in your"
                    + " docker-maven-client's plugin configuration");
        }
        final CompositeImageName compositeImageName = CompositeImageName.create(imageName, imageTags);
        for (final String imageTag : compositeImageName.getImageTags()) {
            final String imageNameWithTag = compositeImageName.getName() + ":" + imageTag;
            log.info("Pushing " + imageNameWithTag);
            docker.push(imageNameWithTag, new AnsiProgressHandler());
        }
    }

    public static void saveImage(DockerClient docker, String imageName,
                                 Path tarArchivePath, Logger log)
            throws DockerException, IOException, InterruptedException {
        log.info(String.format("Save docker image %s to %s.",
                imageName, tarArchivePath.toAbsolutePath()));
        final InputStream is = docker.save(imageName);
        java.nio.file.Files.copy(is, tarArchivePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeImageInfoFile(final DockerBuildInformation buildInfo,
                                          final String tagInfoFile) throws IOException {
        final Path imageInfoPath = Paths.get(tagInfoFile);
        if (imageInfoPath.getParent() != null) {
            Files.createDirectories(imageInfoPath.getParent());
        }
        Files.write(imageInfoPath, buildInfo.toJsonBytes());
    }

    private static class DigestExtractingProgressHandler implements ProgressHandler {

        private final ProgressHandler delegate;
        private String digest;

        DigestExtractingProgressHandler(final ProgressHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void progress(final ProgressMessage message) throws DockerException {
            if (message.digest() != null) {
                digest = message.digest();
            }

            delegate.progress(message);
        }

        public String digest() {
            return digest;
        }
    }
}
