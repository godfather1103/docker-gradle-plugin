import java.io.FileInputStream
import java.util.*

plugins {
    java
    id("io.github.godfather1103.docker-plugin") version "1.0"
}

val p = Properties()
p.load(FileInputStream("../gradle.properties"))
p.forEach { t, u -> project.ext.set(t as String, u) }

group = "${property("plugin.groupId")}"
version = "${property("plugin.version")}"
