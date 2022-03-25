rootProject.name = "docker-gradle-plugin"

pluginManagement {
    includeBuild("./docker-gradle-plugin-main")
}

include(":groovy-dsl-demo")
include(":kotlin-dsl-demo")
project(":groovy-dsl-demo").projectDir = file("example/groovy-dsl-demo")
project(":kotlin-dsl-demo").projectDir = file("example/kotlin-dsl-demo")