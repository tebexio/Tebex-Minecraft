import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

plugins {
    java
    id("com.gradleup.shadow")
    id("net.fabricmc.fabric-loom") version "1.15.5" apply(true)
}

val minecraftVersion = properties["minecraft_version"] as String
val loaderVersion = properties["loader_version"] as String
val fabricVersion = properties["fabric_version"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    shadow(project(":tebex-minecraft-runtime"))
    shadow("io.tebex:tbx:3.0.0")

    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    compileOnly("dev.dejvokep:boosted-yaml:1.3")

    implementation("me.lucko:fabric-permissions-api:0.7.0")
    shadow("me.lucko:fabric-permissions-api:0.7.0") {
        isTransitive = false
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())

}

tasks.named("jar") {
    dependsOn("shadowJar")
}
