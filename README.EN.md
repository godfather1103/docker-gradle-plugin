# docker-gradle-plugin
build, push the gradle plug-in of docker image.

## Usage
1. Introducing relevant plug-ins into gradle
```groovy
// groovy DSL
// Using the plugins DSL:
plugins {
  id "io.github.godfather1103.docker-plugin" version "1.0"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.godfather1103:docker-plugin:1.0"
    }
}
apply plugin: "io.github.godfather1103.docker-plugin"

// kotlin DSL
// Using the plugins DSL:
plugins {
    id("io.github.godfather1103.docker-plugin") version "1.0"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.godfather1103:docker-plugin:1.0")
    }
}
apply(plugin = "io.github.godfather1103.docker-plugin")
```

2. Configure relevant build parameters
```groovy
// groovy DSL
docker {
    dockerBuildDependsOn.add("bootJar")
    dockerDirectory.value(project.projectDir.absolutePath)
    def user = (project.findProperty("docker.username") ?: "").toString()
    def password = (project.findProperty("docker.password") ?: "").toString()
    def email = (project.findProperty("docker.email") ?: "").toString()
    def name = (project.findProperty("docker.demo.imageName") ?: "").toString()
    if (!user.isEmpty() && !password.isEmpty()) {
        auth.value(new AuthConfig(user, password, email))
    }
    imageName.value(name + "-groovy")
    dockerImageTags.add("1.0")
    pushImageTag.value(true)
    pushImage.value(true)
}
// kotlin DSL
docker {
    dockerBuildDependsOn.add("bootJar")
    dockerDirectory.value(project.projectDir.absolutePath)
    val user = (project.findProperty("docker.username") ?: "") as String
    val password = (project.findProperty("docker.password") ?: "") as String
    val email = (project.findProperty("docker.email") ?: "") as String
    val name = (project.findProperty("docker.demo.imageName") ?: "demo") as String
    if (user.isNotEmpty() && password.isNotEmpty()) {
        auth.value(AuthConfig(user, password, email))
    }
    imageName.value("$name-kotlin")
    dockerImageTags.add("1.0")
    pushImageTag.value(true)
    pushImage.value(true)
}
```

## Epilogue
idea from [docker-maven-plugin](https://github.com/spotify/docker-maven-plugin)

