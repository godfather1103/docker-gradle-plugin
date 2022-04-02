plugins {
    java
    id("com.gradle.plugin-publish") version "0.15.0"
    `java-gradle-plugin`
    kotlin("jvm") version "1.4.32"
    id("io.github.godfather1103.gradle-base-plugin") version "1.5"
}

group = "${property("plugin.groupId")}"
version = "${property("plugin.version")}"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(gradleApi())
    implementation("com.spotify:docker-client:8.16.0:shaded")
    implementation("com.google.auth:google-auth-library-oauth2-http:0.6.0")
    implementation("com.typesafe:config:1.2.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    testImplementation("junit:junit:4.13")
    implementation("io.vavr:vavr:0.10.4")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType(Javadoc::class.java) {
    options.encoding = "UTF-8"
}

gradlePlugin {
    plugins {
        create("dockerPlugin") {
            id = "${property("plugin.groupId")}.${property("plugin.artifactId")}"
            implementationClass = "com.github.godfather1103.gradle.DockerPlugin"
            displayName = "${property("plugin.displayName")}"
            description = "${property("plugin.description")}"
        }
    }
}

pluginBundle {
    website = "https://github.com/godfather1103"
    vcsUrl = "https://github.com/godfather1103/docker-gradle-plugin"
    description = "${property("plugin.description")}"
    (plugins){
        "dockerPlugin" {
            displayName = "${property("plugin.displayName")}"
            description = "${property("plugin.description")}"
            tags = listOf("docker", "build docker image", "push docker image")
            version = "${property("plugin.version")}"
        }
    }
    mavenCoordinates {
        groupId = "${property("plugin.groupId")}"
        artifactId = "${property("plugin.artifactId")}"
        version = "${property("plugin.version")}"
        description = "${property("plugin.description")}"
    }
}