import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

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

val dockerJavaVersion = "3.3.2"

dependencies {
    implementation(gradleApi())
    implementation("com.github.docker-java:docker-java-core:${dockerJavaVersion}")
    implementation("com.github.docker-java:docker-java-transport-netty:${dockerJavaVersion}")
    testImplementation("com.github.docker-java:docker-java-transport-okhttp:${dockerJavaVersion}")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    testImplementation("junit:junit:4.13.2")
    implementation("io.vavr:vavr:0.10.4")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType(Javadoc::class.java) {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "1.8"
    }
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