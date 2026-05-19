import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.kyori.blossom") version "2.1.0"
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", rootProject.version.toString())
            }
        }
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation("net.sf.trove4j:trove4j:3.0.3")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks {
    compileJava {
        options.release.set(17)
        options.encoding = Charsets.UTF_8.name()
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("gnu.trove4j", "io.tebex.plugin.libs.trove4j")
    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3")
    relocate("okio", "io.tebex.plugin.libs.okio")
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
    relocate("kotlin", "io.tebex.plugin.libs.kotlin")
    relocate("com.google.gson", "io.tebex.plugin.libs.gson")
    minimize()
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))