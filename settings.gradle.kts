pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "TebexPlugin"

listOf("sdk", "bukkit", "bungeecord", "velocity", "fabric").forEach(::include)