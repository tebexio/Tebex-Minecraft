import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven {
        name = "william278"
        url = uri("https://repo.william278.net/releases")
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation("com.github.cryptomorin:XSeries:9.3.1") { isTransitive = false }
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
    compileOnly("net.william278.husksync:husksync-bukkit:3.8.7+1.21.4")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3")
    relocate("okio", "io.tebex.plugin.libs.okio")
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
    relocate("kotlin", "io.tebex.plugin.libs.kotlin")
    relocate("com.google.gson", "io.tebex.plugin.libs.gson")
    relocate("com.cryptomorin.xseries", "io.tebex.plugin.libs.xseries")
    minimize()
}
