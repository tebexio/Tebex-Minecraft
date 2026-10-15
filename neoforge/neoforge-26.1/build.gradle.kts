import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

plugins {
    java
    id("com.gradleup.shadow")
    id("net.neoforged.moddev") version "2.0.141"
}

val neoVersion = properties["neo_version"] as String
val modId = properties["mod_id"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

neoForge {
    version = neoVersion

    mods {
        create(modId) {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

dependencies {
    shadow(project(":tebex-minecraft-runtime"))
    shadow("io.tebex:tbx:3.0.0")

    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())

}

tasks.named("jar") {
    dependsOn("shadowJar")
}
