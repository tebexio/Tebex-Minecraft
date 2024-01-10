import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

plugins {
    java
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":sdk"))
    implementation("net.sf.trove4j:trove4j:3.0.3") // Add trove4j dependency
    compileOnly("net.md-5:bungeecord-api:1.20-R0.2-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("gnu.trove4j", "io.tebex.plugin.libs.trove4j") // Relocate trove4j
    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3") // Relocate okio (okhttp dependency)
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml") // Relocate boostedyaml
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains") // Relocate jetbrains
    relocate("kotlin", "io.tebex.plugin.libs.kotlin") // Relocate jetbrains
}