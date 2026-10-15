import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.withGroovyBuilder

val buildCommit = rootProject.extra["buildCommit"] as String
val distributableProjects = rootProject.subprojects.filter { it.name != "tebex-minecraft-runtime" }

val collectBuilds =
    rootProject.tasks.register("collectBuilds", Sync::class.java) {
        group = "build"
        description = "Builds every distributable and copies it into the top-level builds directory."
        into(rootProject.layout.projectDirectory.dir("builds"))
    }

distributableProjects.forEach { platform ->
    val shadowJar = platform.tasks.named("shadowJar")
    shadowJar.configure {
        val archiveTask = this as AbstractArchiveTask
        archiveTask.archiveFileName.set(
            "tebex-${platform.name}-${rootProject.version}-$buildCommit.jar")
        withGroovyBuilder {
            "relocate"("okhttp3", "io.tebex.plugin.libs.okhttp3")
            "relocate"("okio", "io.tebex.plugin.libs.okio")
            "relocate"("kotlin", "io.tebex.plugin.libs.kotlin")
            "relocate"("com.google.gson", "io.tebex.plugin.libs.gson")
            "relocate"("io.gsonfire", "io.tebex.plugin.libs.gsonfire")
            "relocate"("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
            "relocate"("javax.annotation", "io.tebex.plugin.libs.javax.annotation")
            "relocate"("org.intellij.lang.annotations", "io.tebex.plugin.libs.intellij")
            "relocate"("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
            "relocate"("com.cryptomorin.xseries", "io.tebex.plugin.libs.xseries")
        }

        // Reflective SDK adapters must not be removed by minimization.
        doLast {
            val archive = archiveTask.archiveFile.get().asFile
            val temporary = archive.resolveSibling("${archive.name}.tmp")
            ZipInputStream(archive.inputStream()).use { input ->
                ZipOutputStream(temporary.outputStream()).use { output ->
                    var entry = input.nextEntry
                    while (entry != null) {
                        val replacement = ZipEntry(entry.name)
                        if (entry.time >= 0) replacement.time = entry.time
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
            Files.move(
                temporary.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    if (platform.name == "forge-1.20.1") {
        val artifactName = "tebex-${platform.name}-${rootProject.version}-$buildCommit.jar"
        platform.afterEvaluate {
            val reobfuscated = tasks.named("reobfShadowJar", Jar::class.java)
            collectBuilds.configure {
                dependsOn(reobfuscated)
                from(reobfuscated.flatMap { it.archiveFile }) { rename { artifactName } }
            }
        }
    } else {
        collectBuilds.configure {
            dependsOn(shadowJar)
            from(shadowJar.flatMap { (it as AbstractArchiveTask).archiveFile })
        }
    }
}

val cleanProjectOutputs =
    rootProject.tasks.register("cleanProjectOutputs") {
        group = "build"
        description = "Removes generated Gradle outputs from platform and runtime projects."
        doLast {
            (listOf(rootProject) + rootProject.subprojects).forEach { project ->
                rootProject.delete(project.layout.buildDirectory.get().asFile)
            }
        }
    }

rootProject.tasks.register("buildDistributables") {
    group = "build"
    description = "Collects the distributable JARs, then removes intermediate project outputs."
    dependsOn(collectBuilds)
    dependsOn(distributableProjects.map { it.tasks.named("build") })
    dependsOn(rootProject.project(":tebex-minecraft-runtime").tasks.named("build"))
    finalizedBy(cleanProjectOutputs)
}

val formatAndBuildDistributables =
    rootProject.tasks.register("formatAndBuildDistributables") {
        group = "build"
        description = "Formats sources, builds all distributable JARs, and cleans project outputs."
        dependsOn(rootProject.tasks.named("spotlessApply"))
        finalizedBy(rootProject.tasks.named("buildDistributables"))
    }

rootProject.tasks.named("build") {
    dependsOn(formatAndBuildDistributables)
}
