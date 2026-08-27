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
    maven("https://maven.enginehub.org/repo/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // WorldGuard API - exclude conflicting transitive deps
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("it.unimi.dsi", "fastutil")
        exclude("org.apache.logging.log4j")
    }

    // WorldEdit API - exclude conflicting transitive deps
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.12") {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("it.unimi.dsi", "fastutil")
        exclude("org.apache.logging.log4j")
    }

    // MythicMobs API
    compileOnly("io.lumine:Mythic-Dist:5.6.0")

    // SQLite JDBC for progress persistence
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    implementation("org.yaml:snakeyaml:2.3")
}

// Force versions to match Paper's shaded dependencies to avoid conflicts
configurations.all {
    resolutionStrategy {
        // Guava - Paper uses 32.1.2-jre (Mojang provides)
        force("com.google.guava:guava:32.1.2-jre")

        // Gson - Paper uses 2.10.1 (Mojang provides)
        force("com.google.code.gson:gson:2.10.1")

        // FastUtil - Paper uses 8.5.6 (Mojang provides)
        force("it.unimi.dsi:fastutil:8.5.6")

        // Log4j - Paper uses 2.22.1 (Mojang provides)
        force("org.apache.logging.log4j:log4j-bom:2.22.1")
        force("org.apache.logging.log4j:log4j-api:2.22.1")
        force("org.apache.logging.log4j:log4j-core:2.22.1")
        force("org.apache.logging.log4j:log4j-slf4j-impl:2.22.1")

        // Netty - Paper uses 4.1.100.Final
        force("io.netty:netty-common:4.1.100.Final")
        force("io.netty:netty-buffer:4.1.100.Final")
        force("io.netty:netty-codec:4.1.100.Final")
        force("io.netty:netty-handler:4.1.100.Final")
        force("io.netty:netty-resolver:4.1.100.Final")
        force("io.netty:netty-transport:4.1.100.Final")
        force("io.netty:netty-transport-native-epoll:4.1.100.Final")
        force("io.netty:netty-transport-native-unix-common:4.1.100.Final")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-Xlint:unchecked", "-Xlint:deprecation")
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
            "Plugin-Depend" to "WorldGuard, MythicMobs"
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
            "Plugin-Depend" to "WorldGuard, MythicMobs"
        )
    }
}