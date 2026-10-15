import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

group = rootProject.group
version = rootProject.version

val minecraftVersionNumber = properties["minecraft_version"] as String
val forgeVersionNumber = properties["forge_version"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

legacyForge {
    setVersion("$minecraftVersionNumber-$forgeVersionNumber")
    mods {
        create("tebex") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    shadow(project(":tebex-minecraft-runtime"))
    shadow("io.tebex:tbx:3.0.0")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("processResources", ProcessResources::class.java) {
    val replacements =
        mapOf(
            "minecraft_version" to minecraftVersionNumber,
            "minecraft_version_range" to properties["minecraft_version_range"],
            "forge_version" to forgeVersionNumber,
            "forge_version_range" to properties["forge_version_range"],
            "loader_version_range" to properties["loader_version_range"],
            "mod_id" to properties["mod_id"],
            "mod_name" to properties["mod_name"],
            "mod_license" to properties["mod_license"],
            "mod_version" to rootProject.version,
            "mod_authors" to properties["mod_authors"],
            "mod_description" to properties["mod_description"],
        )
    inputs.properties(replacements)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replacements + mapOf("project" to project))
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())
}

// Forge 1.20.1 distribution must remain the reobfuscated shadow artifact.
obfuscation {
    reobfuscate(tasks.named("shadowJar", ShadowJar::class.java), sourceSets.main.get())
}

tasks.named("jar") {
    dependsOn("shadowJar")
}
