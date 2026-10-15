import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

plugins {
    java
    id("com.gradleup.shadow")
    id("net.neoforged.gradle.userdev") version "7.1.36"
}

val neoVersion = properties["neo_version"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("net.neoforged:neoforge:$neoVersion")
    shadow(project(":tebex-minecraft-runtime"))
    shadow("io.tebex:tbx:3.0.0")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())

}
