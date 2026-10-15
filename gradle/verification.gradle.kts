import java.net.URLClassLoader
import java.util.Properties
import java.util.jar.JarFile

val buildCommit = rootProject.extra["buildCommit"] as String
val distributableProjects = rootProject.subprojects.filter { it.name != "tebex-minecraft-runtime" }

fun entrypointName(projectName: String): String =
    when {
        projectName == "bukkit" -> "TebexBukkitPlugin"
        projectName == "folia" -> "TebexFoliaPlugin"
        projectName == "bungeecord" -> "TebexBungeePlugin"
        projectName == "velocity" -> "TebexVelocityPlugin"
        projectName.startsWith("fabric-") -> "TebexFabricPlugin"
        projectName.startsWith("forge-") -> "TebexForgePlugin"
        else -> "TebexNeoForgePlugin"
    }

fun adapterName(projectName: String): String =
    when {
        projectName == "bukkit" -> "BukkitPlatform"
        projectName == "folia" -> "FoliaPlatform"
        projectName == "bungeecord" -> "BungeePlatform"
        projectName == "velocity" -> "VelocityPlatform"
        projectName.startsWith("fabric-") -> "FabricPlatform"
        projectName.startsWith("forge-") -> "ForgePlatform"
        else -> "NeoForgePlatform"
    }

fun descriptorName(projectName: String): String =
    when {
        projectName == "bukkit" || projectName == "folia" -> "plugin.yml"
        projectName == "bungeecord" -> "bungee.yml"
        projectName == "velocity" -> "velocity-plugin.json"
        projectName.startsWith("fabric-") -> "fabric.mod.json"
        projectName.startsWith("neoforge-") && projectName != "neoforge-1.20.2" ->
            "META-INF/neoforge.mods.toml"
        else -> "META-INF/mods.toml"
    }

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Guards variant ownership, navigation shape, and the official SDK boundary."
    doLast {
        listOf("modded-common", "bukkit-common", "minecraft-common").forEach { obsolete ->
            check(!rootProject.file(obsolete).exists()) { "$obsolete must not hide variant-owned code" }
        }

        val sourceOverlay = Regex("""java\.srcDir\s*\(\s*rootProject\.file""")
        val buildScripts = listOf(rootProject.buildFile) + rootProject.subprojects.map { it.buildFile }
        check(buildScripts.none { sourceOverlay.containsMatchIn(it.readText()) }) {
            "Variant source trees must not be injected from another project"
        }
        check(distributableProjects.all { it.buildFile.name == "build.gradle.kts" }) {
            "Every distributable must use Kotlin DSL"
        }

        val allSources =
            rootProject.subprojects.flatMap { project ->
                rootProject.fileTree(project.file("src/main/java")) { include("**/*.java") }.files
            }
        val forbiddenTransport =
            listOf(
                "import io.tebex.sdk.",
                "import okhttp3.",
                "import java.net.HttpURLConnection",
                "import java.net.http.",
            )
        allSources.forEach { source ->
            val text = source.readText()
            check(forbiddenTransport.none(text::contains)) {
                "$source: obsolete SDK or custom Tebex transport dependency"
            }
            check(!text.contains("extends TebexRuntime") && !text.contains("BasePluginPlatform")) {
                "$source: native platforms must compose the runtime"
            }
        }

        val runtimeRoot = rootProject.project(":tebex-minecraft-runtime").file("src/main/java")
        val nativeImports =
            listOf(
                "import org.bukkit.",
                "import net.md_5.",
                "import com.velocitypowered.",
                "import net.fabricmc.",
                "import net.minecraftforge.",
                "import net.neoforged.",
                "import net.minecraft.",
            )
        rootProject.fileTree(runtimeRoot) { include("**/*.java") }.files.forEach { source ->
            val text = source.readText()
            check(nativeImports.none(text::contains)) {
                "$source: platform-neutral runtime must not import a native Minecraft API"
            }
        }
        val runtimeSource =
            rootProject.file(
                "tebex-minecraft-runtime/src/main/java/io/tebex/minecraft/runtime/TebexRuntime.java")
        check(runtimeSource.isFile && runtimeSource.readText().contains("public final class TebexRuntime")) {
            "TebexRuntime must be final and live in io.tebex.minecraft.runtime"
        }
        val obsoleteRuntimeAliases =
            listOf(
                " void halt(",
                " void configure(",
                "setStoreInfo(",
                "setServerInformation(",
                "getQueuedPlayers(",
                "getJoinEvents(",
            )
        check(obsoleteRuntimeAliases.none(runtimeSource.readText()::contains)) {
            "TebexRuntime contains an obsolete alias or mutable collection accessor"
        }

        distributableProjects.forEach { platform ->
            val expectedFamily =
                when {
                    platform.name.startsWith("fabric-") -> "fabric"
                    platform.name.startsWith("forge-") -> "forge"
                    platform.name.startsWith("neoforge-") -> "neoforge"
                    else -> null
                }
            if (expectedFamily != null) {
                check(platform.projectDir.parentFile.canonicalFile == rootProject.file(expectedFamily).canonicalFile) {
                    "${platform.name}: expected source directory under $expectedFamily/"
                }
            }
            val sourceRoot = platform.file("src/main/java")
            val pluginRoot = platform.file("src/main/java/io/tebex/plugin")
            val localSources = rootProject.fileTree(sourceRoot) { include("**/*.java") }.files
            val entrypoint = entrypointName(platform.name)
            val adapter = adapterName(platform.name)
            val rootJava = pluginRoot.listFiles { file -> file.extension == "java" }.orEmpty()
            check(rootJava.map { it.nameWithoutExtension }.toSet() == setOf(entrypoint, adapter)) {
                "${platform.name}: root package must contain only $entrypoint and $adapter"
            }
            check(pluginRoot.resolve("$entrypoint.java").isFile) {
                "${platform.name}: missing loader entrypoint $entrypoint"
            }
            val adapterFile = pluginRoot.resolve("$adapter.java")
            check(adapterFile.isFile && adapterFile.readText().contains("implements PlatformHost")) {
                "${platform.name}: $adapter must directly implement PlatformHost"
            }
            check(localSources.none { source ->
                source.name.endsWith("Bootstrap.java") ||
                    source.name == "ModdedPlatform.java" ||
                    source.name.endsWith("PluginPlatform.java")
            }) {
                "${platform.name}: obsolete bootstrap or adapter layer found"
            }
            localSources.forEach { source ->
                check(source.readText().startsWith("package io.tebex.plugin")) {
                    "${platform.name}: native source outside io.tebex.plugin: $source"
                }
            }
            val configuredSources =
                platform.extensions
                    .getByType<JavaPluginExtension>()
                    .sourceSets
                    .getByName("main")
                    .allJava
                    .files
            check(configuredSources.all { source ->
                source.canonicalFile.toPath().startsWith(platform.projectDir.canonicalFile.toPath())
            }) {
                "${platform.name}: production Java sources must live inside the variant"
            }
            check(platform.buildFile.readText().contains("io.tebex:tbx:3.0.0")) {
                "${platform.name}: declare the pinned Tebex SDK dependency directly"
            }
            check(platform.file("README.md").isFile) {
                "${platform.name}: missing local implementation README"
            }

            val descriptorEntrypoint =
                when {
                    platform.name == "bukkit" || platform.name == "folia" ->
                        platform.file("src/main/resources/plugin.yml")
                    platform.name == "bungeecord" -> platform.file("src/main/resources/bungee.yml")
                    platform.name.startsWith("fabric-") ->
                        platform.file("src/main/resources/fabric.mod.json")
                    else -> null
                }
            if (descriptorEntrypoint != null)
                check(descriptorEntrypoint.readText().contains("io.tebex.plugin.$entrypoint")) {
                    "${platform.name}: descriptor entrypoint changed"
                }
        }
    }
}

tasks.register("verifyArtifacts") {
    group = "verification"
    dependsOn("collectBuilds")
    doLast {
        check(distributableProjects.size == 15) {
            "Expected 15 distributables, found ${distributableProjects.size}"
        }
        val expectedNames =
            distributableProjects
                .map { "tebex-${it.name}-${rootProject.version}-$buildCommit.jar" }
                .toSet()
        val actualNames =
            rootProject.file("builds").listFiles { file -> file.extension == "jar" }.orEmpty()
                .map { it.name }
                .toSet()
        check(actualNames == expectedNames) {
            "builds/ does not contain the exact distributable set: $actualNames"
        }

        distributableProjects.forEach { platform ->
            val archive =
                rootProject.layout.projectDirectory
                    .file("builds/tebex-${platform.name}-${rootProject.version}-$buildCommit.jar")
                    .asFile
            JarFile(archive).use { jar ->
                val required =
                    listOf(
                        "io/tebex/http/PluginApi.class",
                        "io/tebex/model/QueuedCommand.class",
                        "io/tebex/headless/model/Basket.class",
                        "io/tebex/minecraft/runtime/SdkSession.class",
                        "io/tebex/minecraft/runtime/TebexRuntime.class",
                        "io/tebex/minecraft/runtime/CommandDelivery.class",
                        "io/tebex/plugin/libs/gson/Gson.class",
                        "io/tebex/plugin/libs/gsonfire/GsonFireBuilder.class",
                        "io/tebex/plugin/libs/intellij/MagicConstant.class",
                        "io/tebex/plugin/libs/javax/annotation/Nullable.class",
                        "io/tebex/plugin/libs/okhttp3/OkHttpClient.class",
                    )
                required.forEach { entry ->
                    check(jar.getEntry(entry) != null) { "${platform.name}: missing $entry" }
                }
                check(jar.entries().asSequence().none { it.name.startsWith("io/tebex/sdk/") }) {
                    "${platform.name}: legacy SDK classes packaged"
                }
                listOf(
                        "io/tebex/http/PluginApi.class",
                        "io/tebex/minecraft/runtime/CommandDelivery.class",
                    )
                    .forEach { entry ->
                        val bytes = jar.getInputStream(jar.getEntry(entry)).use { it.readBytes() }
                        val major = ((bytes[6].toInt() and 255) shl 8) or (bytes[7].toInt() and 255)
                        check(major == 52) {
                            "${platform.name}: shared code is not Java 8 ($major)"
                        }
                    }

                val descriptor = descriptorName(platform.name)
                check(jar.getEntry(descriptor) != null) { "${platform.name}: missing $descriptor" }
                val descriptorText =
                    jar.getInputStream(jar.getEntry(descriptor)).use { it.bufferedReader().readText() }
                check(descriptorText.contains(rootProject.version.toString())) {
                    "${platform.name}: stale descriptor release version"
                }
                val entrypoint = entrypointName(platform.name)
                val nativeClass = jar.getJarEntry("io/tebex/plugin/$entrypoint.class")
                check(nativeClass != null) { "${platform.name}: missing entrypoint $entrypoint" }
                check(jar.getJarEntry("io/tebex/plugin/${adapterName(platform.name)}.class") != null) {
                    "${platform.name}: missing local platform adapter"
                }
                if (
                    platform.name == "bukkit" ||
                        platform.name == "folia" ||
                        platform.name == "bungeecord" ||
                        platform.name == "velocity" ||
                        platform.name.startsWith("fabric-")
                ) {
                    check(descriptorText.contains("io.tebex.plugin.$entrypoint")) {
                        "${platform.name}: packaged descriptor entrypoint changed"
                    }
                }

                val buildInfo = Properties()
                jar.getInputStream(jar.getJarEntry("tebex-build.properties")).use(buildInfo::load)
                check(buildInfo.getProperty("version") == rootProject.version.toString()) {
                    "${platform.name}: stale shared release version"
                }
                val nativeBytes = jar.getInputStream(nativeClass).use { it.readBytes() }
                val nativeMajor =
                    ((nativeBytes[6].toInt() and 255) shl 8) or (nativeBytes[7].toInt() and 255)
                val expectedMajor =
                    platform.extensions
                        .getByType<JavaPluginExtension>()
                        .toolchain
                        .languageVersion
                        .get()
                        .asInt() + 44
                check(nativeMajor == expectedMajor) {
                    "${platform.name}: unexpected native Java target $nativeMajor"
                }

                val allowedClassPrefixes = mutableListOf("io/tebex/")
                if (platform.name.startsWith("fabric-"))
                    allowedClassPrefixes.add("me/lucko/fabric/api/permissions/")
                val unexpectedClasses =
                    jar.entries()
                        .asSequence()
                        .filter { it.name.endsWith(".class") }
                        .map { it.name }
                        .filter { name -> allowedClassPrefixes.none(name::startsWith) }
                        .take(10)
                        .toList()
                check(unexpectedClasses.isEmpty()) {
                    "${platform.name}: unrelocated dependency classes: ${unexpectedClasses.joinToString()}"
                }

                if (platform.name == "forge-1.20.1") {
                    val commandClass =
                        jar.getJarEntry("io/tebex/plugin/command/TebexCommandExecutor.class")
                    check(commandClass != null) { "forge-1.20.1: missing command adapter" }
                    val symbols =
                        jar.getInputStream(commandClass).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
                    check(symbols.contains("m_82127_") && !symbols.contains("literal")) {
                        "forge-1.20.1: distributable still uses development mappings"
                    }
                }
            }

            URLClassLoader(
                    arrayOf(archive.toURI().toURL()), ClassLoader.getPlatformClassLoader())
                .use { loader ->
                    loader.loadClass("io.tebex.http.PluginApi").getConstructor().newInstance()
                    loader.loadClass("io.tebex.http.HeadlessApi").getConstructor().newInstance()
                    val gsonClass = loader.loadClass("io.tebex.plugin.libs.gson.Gson")
                    val gson = gsonClass.getConstructor().newInstance()
                    val model = loader.loadClass("io.tebex.model.QueuedCommand")
                    val command =
                        gsonClass
                            .getMethod("fromJson", String::class.java, Class::class.java)
                            .invoke(gson, """{"id":42,"command":"say test"}""", model)
                    check(model.getMethod("getId").invoke(command) == 42) {
                        "${platform.name}: shaded model decoding failed"
                    }
                }
        }
    }
}

tasks.register("verifyMigration") {
    group = "verification"
    description = "Runs runtime/SDK tests and verifies every distributable platform jar."
    dependsOn(
        ":tebex-minecraft-runtime:test",
        ":bukkit:test",
        ":bungeecord:test",
        ":velocity:test",
        "verifyArtifacts",
        "verifyArchitecture",
    )
    dependsOn(gradle.includedBuild("tebex-java-sdk").task(":tbx:test"))
    dependsOn(gradle.includedBuild("tebex-java-sdk").task(":headless-api:test"))
    finalizedBy(rootProject.tasks.named("cleanProjectOutputs"))
}
