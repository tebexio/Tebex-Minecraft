import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    id("com.gradleup.shadow") version "8.3.1"
}

defaultTasks("shadowJar")

group = "io.tebex"
version = "2.3.2"

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
    }

    tasks.named("processResources", Copy::class.java) {
        val props = mapOf("version" to rootProject.version, "@VERSION@" to rootProject.version)
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

val fabric1211Project = project(":fabric-1.21.1")
fabric1211Project.configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java {
                srcDir("src/main/kotlin")
            }
        }
    }
}

val fabric1214Project = project(":fabric-1.21.4")
fabric1214Project.configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java {
                srcDir("src/main/kotlin")
            }
        }
    }
}

val fabric1215Project = project(":fabric-1.21.5")
fabric1215Project.configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java {
                srcDir("src/main/kotlin")
            }
        }
    }
}

val fabric1216Project = project(":fabric-1.21.6")
fabric1216Project.configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java {
                srcDir("src/main/kotlin")
            }
        }
    }
}

val fabric1217Project = project(":fabric-1.21.7")
fabric1217Project.configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java {
                srcDir("src/main/kotlin")
            }
        }
    }
}