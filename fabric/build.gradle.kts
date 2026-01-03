import net.fabricmc.loom.task.RemapJarTask

plugins {
    idea
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val minecraftVersion: String = property("minecraft_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val fabricVersion : String = property("fabric_version") as String
val architecturyVersion: String = property("architectury_version") as String

val common: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val developmentFabric: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentFabric.extendsFrom(common)
}

repositories {
    maven("https://mvn.devos.one/releases/") // Porting Lib
    maven("https://mvn.devos.one/snapshots/") // Create Fabric
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") // Forge Config API
    maven("https://maven.jamieswhiteshirt.com/libs-release") // Reach Entity Attributes
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    modApi("dev.architectury:architectury-fabric:${architecturyVersion}")

    modImplementation("com.simibubi.create:create-fabric-${minecraftVersion}:${property("create_fabric_version")}")

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(":common", configuration = "transformProductionFabric"))
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveFileName = "${base.archivesName.get()}.jar"
}