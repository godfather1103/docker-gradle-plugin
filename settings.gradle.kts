rootProject.name = "docker-gradle-plugin"

pluginManagement {
    includeBuild("./docker-gradle-plugin-main")
}

include(":groovy-dsl-demo")
include(":kotlin-dsl-demo")