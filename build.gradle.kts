plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.dungeongates"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mythicmobs.net/snapshots/")
    maven("https://ci.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit-platform:7.0.12")
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.12")
    compileOnly("io.github.mythicmobs:mythicmobs-api:5.6.0")
    compileOnly("me.clip:placeholderapi:2.11.7")
    
    implementation("org.yaml:snakeyaml:2.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
}

tasks.shadowJar {
    archiveBaseName.set("DungeonGates")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "com.dungeongates.DungeonGatesPlugin",
            "Plugin-Name" to "DungeonGates",
            "Plugin-Version" to project.version.toString(),
            "Plugin-Main" to "com.dungeongates.DungeonGatesPlugin",
            "Plugin-API-Version" to "1.21",
            "Plugin-Depend" to "WorldGuard, MythicMobs",
            "Plugin-SoftDepend" to "PlaceholderAPI"
        )
    }
    mergeServiceFiles()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.dungeongates.DungeonGatesPlugin",
            "Plugin-Name" to "DungeonGates",
            "Plugin-Version" to project.version.toString(),
            "Plugin-Main" to "com.dungeongates.DungeonGatesPlugin",
            "Plugin-API-Version" to "1.21",
            "Plugin-Depend" to "WorldGuard, MythicMobs",
            "Plugin-SoftDepend" to "PlaceholderAPI"
        )
    }
}

tasks.build.dependsOn(tasks.shadowJar)