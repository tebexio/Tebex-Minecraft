plugins {
    id("net.kyori.blossom") version "2.1.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
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
    implementation("net.sf.trove4j:trove4j:3.0.3") // Add trove4j dependency

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks {
    compileJava {
        options.release.set(17)
        options.encoding = Charsets.UTF_8.name()
    }
    shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get())

        relocate("gnu.trove4j", "net.analyse.plugin.libs.trove4j") // Relocate trove4j
        relocate("okhttp3", "net.analyse.plugin.libs.okhttp3") // Relocate okhttp
        relocate("okio", "net.analyse.plugin.libs.okio") // Relocate okio (okhttp dependency)
        relocate("dev.dejvokep.boostedyaml", "net.analyse.plugin.libs.boostedyaml") // Relocate boostedyaml
        relocate("org.jetbrains.annotations", "net.analyse.plugin.libs.jetbrains") // Relocate jetbrains
        relocate("kotlin", "net.analyse.plugin.libs.kotlin") // Relocate jetbrains
        minimize()
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
