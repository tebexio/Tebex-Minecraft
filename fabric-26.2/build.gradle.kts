import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

fun gitCommitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (process.exitValue() == 0) result else "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

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
    shadow(project(":sdk"))

    shadow("com.github.cryptomorin:XSeries:9.3.1") {
        isTransitive = false
    }

    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    compileOnly("dev.dejvokep:boosted-yaml:1.3")

    implementation("me.lucko:fabric-permissions-api:0.7.0")
    include("me.lucko:fabric-permissions-api:0.7.0")
    shadow("me.lucko:fabric-permissions-api:0.7.0") {
        isTransitive = false
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())

    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3")
    relocate("okio", "io.tebex.plugin.libs.okio")
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
    relocate("kotlin", "io.tebex.plugin.libs.kotlin")
    relocate("com.google.gson", "io.tebex.plugin.libs.gson")
    minimize()

    archiveFileName.set("tebex-${project.name}-${rootProject.version}-${gitCommitHash()}.jar")
}

tasks.named("jar") {
    dependsOn("shadowJar")
}
