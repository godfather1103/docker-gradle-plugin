import java.io.FileInputStream
import java.util.*

plugins {
    java
    id("com.gradle.plugin-publish") version "0.15.0"
    `java-gradle-plugin`
    kotlin("jvm") version "1.4.32"
}

val p = Properties()
p.load(FileInputStream("../gradle.properties"))
p.forEach { t, u -> project.ext.set(t as String, u) }

group = "${property("plugin.groupId")}"
version = "${property("plugin.version")}"

dependencies {
    implementation(gradleApi())
    implementation("com.spotify:docker-client:8.16.0:shaded")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.5.3")
    implementation("com.typesafe:config:1.4.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")
    testImplementation("junit:junit:4.13.2")
}

java {
    withJavadocJar()
    withSourcesJar()
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