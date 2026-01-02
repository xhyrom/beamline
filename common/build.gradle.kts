plugins {
    idea
    java
    id("java-library")
    id("xyz.wagyourtail.unimined")
    id("com.github.johnrengelman.shadow")
}

val minecraftVersion: String = property("minecraft_version") as String
val neoforgeVersion: String = property("neoforge_version") as String

val shadowBundle: Configuration by configurations.creating
val noRemap: Configuration by configurations.creating

unimined.minecraft {
    version(minecraftVersion)

    mappings {
        mojmap()
    }

    defaultRemapJar = false
}

repositories {
    unimined.spongeMaven()
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    implementation("com.simibubi.create:create-${minecraftVersion}:${property("create_neoforge_version")}:slim") { isTransitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:${property("ponder_version")}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${minecraftVersion}:${property("flywheel_version")}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${minecraftVersion}:${property("flywheel_version")}")
    implementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "no-remap"
    archiveFileName = "${base.archivesName.get()}-${archiveClassifier.get()}.jar"
}

artifacts {
    add(noRemap.name, tasks.shadowJar)
}