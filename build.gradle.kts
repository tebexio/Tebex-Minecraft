import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    id("com.gradleup.shadow") version "9.4.1"
}

defaultTasks("collectBuilds")

group = "io.tebex"
version = "2.4.6"

val collectBuilds = tasks.register("collectBuilds", Sync::class.java) {
    group = "build"
    description = "Builds all shaded jars and copies them into the top-level builds directory."
    into(layout.projectDirectory.dir("builds"))
}

tasks.register("processSources", Copy::class.java) {
    val props = mapOf("@VERSION@" to rootProject.version)
    from("src/main/java")
    into(layout.buildDirectory.dir("processedSources")) // Destination for processed sources
    filteringCharset = "UTF-8"
    expand(props)
}

tasks.withType<JavaCompile> {
    dependsOn("processSources")
    source = fileTree(layout.buildDirectory.dir("processedSources"))
}

subprojects {
    plugins.apply("java")
    plugins.apply("com.gradleup.shadow")
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.named("shadowJar", ShadowJar::class.java) {
        archiveFileName.set("tebex-${project.name}-${rootProject.version}-${gitCommitHash()}.jar")
        doLast {
            val archive = archiveFile.get().asFile
            val tempArchive = archive.resolveSibling("${archive.name}.tmp")

            ZipInputStream(archive.inputStream()).use { input ->
                ZipOutputStream(tempArchive.outputStream()).use { output ->
                    var entry = input.nextEntry
                    while (entry != null) {
                        val replacement = ZipEntry(entry.name)
                        if (entry.time >= 0) {
                            replacement.time = entry.time
                        }
                        output.putNextEntry(replacement)

                        if (entry.name.equals("META-INF/MANIFEST.MF", ignoreCase = true)) {
                            val manifest = Manifest(input)
                            manifest.mainAttributes.remove(Attributes.Name("Multi-Release"))
                            manifest.write(output)
                        } else {
                            input.copyTo(output)
                        }

                        output.closeEntry()
                        input.closeEntry()
                        entry = input.nextEntry
                    }
                }
            }

            Files.move(tempArchive.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    collectBuilds.configure {
        val shadowJarTask = tasks.named("shadowJar", ShadowJar::class.java)
        dependsOn(shadowJarTask)
        from(shadowJarTask.flatMap { it.archiveFile })
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "spigotmc-repo"
        }
        maven("https://hub.spigotmc.org/nexus/content/groups/public/") {
            name = "spigotmc-public"
        }
        maven("https://oss.sonatype.org/content/groups/public/") {
            name = "sonatype"
        }
        maven("https://repo.opencollab.dev/main/") {
            name = "opencollab-snapshot-repo"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "paper-repo"
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "extendedclip-repo"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "sonatype-snapshots"
        }
        maven("https://maven.nucleoid.xyz/") {
            name = "nucleoid"
        }
        maven("https://maven.neoforged.net/releases") {
            name = "neoforged"
        }
    }

    tasks.named("processResources", Copy::class.java) {
        val props = mutableMapOf<String, Any>(
            "version" to rootProject.version,
            "@VERSION@" to rootProject.version,
            "mod_version" to rootProject.version
        )
        listOf(
            "minecraft_version",
            "minecraft_version_range",
            "forge_version",
            "forge_version_range",
            "neo_version",
            "neo_version_range",
            "loader_version_range",
            "mod_id",
            "mod_name",
            "mod_license",
            "mod_authors",
            "mod_description"
        ).forEach { key ->
            project.findProperty(key)?.let { props[key] = it }
        }
        inputs.properties(props)
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        filesNotMatching("**/*.zip") {
            expand(props)
        }
    }
}

project(":folia") {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

listOf(
    "fabric-26.1",
    "fabric-26.2"
).forEach { projectName ->
    val fabricProject = project(":$projectName")
    fabricProject.configure<JavaPluginExtension> {
        sourceSets {
            getByName("main") {
                java {
                    srcDir("src/main/kotlin")
                }
            }
        }
    }
}

project(":forge-26.1") {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}

listOf(
    "forge-1.20.1",
).forEach { projectName ->
    project(":$projectName") {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

listOf(
    "forge-1.21.1",
    "neoforge-1.21.1"
).forEach { projectName ->
    project(":$projectName") {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
}
