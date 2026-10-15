import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.ForgeGradleExtensionForProject
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

group = rootProject.group
version = rootProject.version

val minecraftVersion = properties["minecraft_version"] as String
val forgeVersion = properties["forge_version"] as String
val forgeGradle = extensions.getByType(ForgeGradleExtensionForProject::class.java)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    minecraft.mavenizer(this)
    maven(forgeGradle.forgeMaven)
    maven(forgeGradle.minecraftLibsMaven)
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:$minecraftVersion-$forgeVersion"))
    shadow(project(":tebex-minecraft-runtime"))
    shadow("io.tebex:tbx:3.0.0")
    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("processResources", ProcessResources::class.java) {
    val replacements =
        mapOf(
            "minecraft_version" to minecraftVersion,
            "minecraft_version_range" to properties["minecraft_version_range"],
            "forge_version" to forgeVersion,
            "forge_version_range" to properties["forge_version_range"],
            "loader_version_range" to properties["loader_version_range"],
            "mod_id" to properties["mod_id"],
            "mod_name" to properties["mod_name"],
            "mod_license" to properties["mod_license"],
            "mod_version" to rootProject.version,
            "mod_authors" to properties["mod_authors"],
            "mod_description" to properties["mod_description"],
        )
    inputs.properties(replacements)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replacements + mapOf("project" to project))
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())
}

tasks.named("jar") {
    dependsOn("shadowJar")
}
