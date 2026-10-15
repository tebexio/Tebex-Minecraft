plugins { `java-library` }
group = rootProject.group
version = rootProject.version
repositories { maven("https://libraries.minecraft.net") }
dependencies {
    api("io.tebex:tbx:3.0.0")
    api("dev.dejvokep:boosted-yaml:1.3")
    compileOnly("com.mojang:brigadier:1.3.10")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("com.google.guava:guava:33.3.1-jre")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testImplementation("com.google.guava:guava:33.3.1-jre")
}

