pluginManagement {
    repositories {
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "TebexPlugin"
// Substitute published SDK coordinates with the pinned, unchanged submodule.
includeBuild("tebex-java-sdk")

listOf(
    "tebex-minecraft-runtime",
    "bukkit",
    "bungeecord",
    "velocity",
    "folia",
).forEach(::include)

mapOf(
    "fabric" to listOf("fabric-26.1", "fabric-26.2"),
    "forge" to listOf("forge-1.20.1", "forge-1.21.1", "forge-26.1", "forge-26.2", "forge-26.3"),
    "neoforge" to listOf("neoforge-1.20.2", "neoforge-1.21.1", "neoforge-26.1", "neoforge-26.2"),
).forEach { (family, variants) ->
    variants.forEach { variant ->
        include(variant)
        project(":$variant").projectDir = file("$family/$variant")
    }
}
