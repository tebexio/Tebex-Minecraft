pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "TebexPlugin"

listOf("sdk", "bukkit", "bungeecord", "velocity", "fabric-1.20.1", "fabric-1.20.4", "fabric-1.21.1").forEach(::include)

