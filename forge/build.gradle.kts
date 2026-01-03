import net.fabricmc.loom.task.RemapJarTask

plugins {
    idea
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    forge()
}

val modId: String = property("mod_id") as String
val minecraftVersion: String = property("minecraft_version") as String
val forgeVersion: String = property("forge_version") as String
val architecturyVersion: String = property("architectury_version") as String

loom {
    forge {
        mixinConfig("${modId}.mixins.json")
    }
}

val common: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val developmentForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentForge.extendsFrom(common)
}

dependencies {
    forge("net.minecraftforge:forge:${forgeVersion}")

    modApi("dev.architectury:architectury-forge:${architecturyVersion}")

    modImplementation("com.simibubi.create:create-${minecraftVersion}:${property("create_forge_version")}:slim") { isTransitive = false }
    modImplementation("net.createmod.ponder:Ponder-Forge-${minecraftVersion}:${property("ponder_version")}")
    modCompileOnly("dev.engine-room.flywheel:flywheel-forge-api-${minecraftVersion}:${property("flywheel_version")}")
    modRuntimeOnly("dev.engine-room.flywheel:flywheel-forge-${minecraftVersion}:${property("flywheel_version")}")
    modImplementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")

    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")
    implementation("io.github.llamalad7:mixinextras-forge:0.4.1")

    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(":common", configuration = "transformProductionForge")) {
        isTransitive = false
    }
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveFileName = "${base.archivesName.get()}.jar"
}