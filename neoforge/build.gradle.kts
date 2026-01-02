import org.gradle.kotlin.dsl.named
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    idea
    id("xyz.wagyourtail.unimined")
    id("com.github.johnrengelman.shadow")
}

val modId: String = property("mod_id") as String
val minecraftVersion: String = property("minecraft_version") as String
val neoforgeVersion: String = property("neoforge_version") as String
val createNeoforgeVersion : String = property("create_neoforge_version") as String

val shadowBundle: Configuration by configurations.creating

unimined.minecraft {
    version(minecraftVersion)

    neoForge {
        loader(neoforgeVersion)
        mixinConfig("${modId}.mixins.json")
    }

    defaultRemapJar = true
}

repositories {
    maven("https://jitpack.io")
    unimined.neoForgedMaven()
    unimined.modrinthMaven()
}

dependencies {
    implementation("com.simibubi.create:create-${minecraftVersion}:${property("create_neoforge_version")}:slim") { isTransitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:${property("ponder_version")}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${minecraftVersion}:${property("flywheel_version")}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${minecraftVersion}:${property("flywheel_version")}")
    implementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")

    implementation(project(":common"))
    shadowBundle(project(":common"))
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(tasks.shadowJar)
    asJar {
        inputFile.set(tasks.shadowJar.get().archiveFile)
        archiveFileName = "${base.archivesName.get()}.jar"
    }
}

tasks.build {
    dependsOn(tasks.named("remapJar"))
}