import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":sdk"))
    implementation("com.github.cryptomorin:XSeries:9.3.1") { isTransitive = false }
    implementation("dev.triumphteam:triumph-gui:3.1.2")
    implementation("space.arim.morepaperlib:morepaperlib:0.4.3")

    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("it.unimi", "io.tebex.plugin.libs.fastutil")
    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3")
    relocate("net.kyori", "io.tebex.plugin.libs.kyori")
    relocate("okio", "io.tebex.plugin.libs.okio")
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
    relocate("com.google.gson", "io.tebex.plugin.libs.gson")
    minimize()
}

tasks.register("copyToServer", Copy::class.java) {
    from(project.tasks.named("shadowJar").get().outputs)
    into("${project.rootDir}/MCServer/plugins")

    // rely on the shadowJar task to build the jar
    dependsOn("shadowJar")
}
