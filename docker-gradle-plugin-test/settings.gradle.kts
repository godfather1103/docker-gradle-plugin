rootProject.name = "docker-gradle-plugin-test"

pluginManagement {
    includeBuild("../docker-gradle-plugin-main")
}

include(":groovy-dsl-demo")
include(":kotlin-dsl-demo")