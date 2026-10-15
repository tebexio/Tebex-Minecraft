fun gitCommitHash(): String =
    try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").redirectErrorStream(true).start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (process.exitValue() == 0) result else "unknown"
    } catch (_: Exception) {
        "unknown"
    }

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
    id("com.diffplug.spotless") version "8.10.2"
}

defaultTasks("build")

group = "io.tebex"
version = "3.0.0"
extra["buildCommit"] = gitCommitHash()

repositories { mavenCentral() }

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
    plugins.apply("java")
    plugins.apply("com.gradleup.shadow")
    plugins.apply("com.diffplug.spotless")

    if (project.file("src/test").exists()) {
        dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter:5.10.0")
            add("testImplementation", "org.mockito:mockito-inline:4.11.0")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
        maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
        maven("https://repo.opencollab.dev/main/") { name = "opencollab-snapshot-repo" }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "extendedclip-repo"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "sonatype-snapshots"
        }
        maven("https://maven.nucleoid.xyz/") { name = "nucleoid" }
        maven("https://maven.neoforged.net/releases") { name = "neoforged" }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        dependsOn(rootProject.tasks.named("spotlessApply"))
    }

    tasks.named("processResources", Copy::class.java) {
        val replacements =
            mutableMapOf<String, Any>(
                "version" to rootProject.version,
                "@VERSION@" to rootProject.version,
                "mod_version" to rootProject.version,
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
                "mod_description",
            )
            .forEach { key -> project.findProperty(key)?.let { replacements[key] = it } }
        inputs.properties(replacements)
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filesNotMatching("**/*.zip") { expand(replacements) }
    }
}

project(":folia") {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

apply(from = "gradle/distribution.gradle.kts")
apply(from = "gradle/verification.gradle.kts")
