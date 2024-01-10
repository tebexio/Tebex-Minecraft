import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.kyori.blossom") version "1.2.0"
}

group = rootProject.group
version = rootProject.version

blossom {
    replaceToken("@VERSION@", version)
}

dependencies {
    implementation(project(":sdk"))
    implementation("net.sf.trove4j:trove4j:3.0.3") // Add trove4j dependency

    compileOnly("com.velocitypowered:velocity-api:3.3.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("gnu.trove4j", "io.tebex.plugin.libs.trove4j") // Relocate trove4j
    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3") // Relocate okhttp
    relocate("okio", "io.tebex.plugin.libs.okio") // Relocate okio (okhttp dependency)
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml") // Relocate boostedyaml
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains") // Relocate jetbrains
    relocate("kotlin", "io.tebex.plugin.libs.kotlin") // Relocate jetbrains
    minimize()
}