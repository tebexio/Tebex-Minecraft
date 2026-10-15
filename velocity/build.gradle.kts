plugins {
    id("net.kyori.blossom") version "2.1.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
}

spotless {
    java {
        targetExclude("build/generated/sources/blossom/**")
    }
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
    implementation(project(":tebex-minecraft-runtime"))
    implementation("io.tebex:tbx:3.0.0")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    testImplementation("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
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

    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
