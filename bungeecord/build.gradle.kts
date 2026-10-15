import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":tebex-minecraft-runtime"))
    implementation("io.tebex:tbx:3.0.0")
    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    testImplementation("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())
}
