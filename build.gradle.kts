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
}

dependencies {
    // Paper API (only compile-time dependency)
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    
    // WorldGuard & MythicMobs - use reflection (APIs not in public Maven repos)
    // These are required at runtime via plugin.yml 'depend'
    
    // SnakeYAML - shaded into final JAR
    implementation("org.yaml:snakeyaml:2.3")
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