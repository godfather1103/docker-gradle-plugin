import java.io.FileInputStream
import java.util.*

val p = Properties()
p.load(FileInputStream("../gradle.properties"))
p.forEach { t, u -> project.ext.set(t as String, u) }

plugins {
    java
    id("io.github.godfather1103.docker-plugin") version "1.0"
}

group = "${property("plugin.groupId")}"
version = "${property("plugin.version")}"
