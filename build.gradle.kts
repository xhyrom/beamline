import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("java")
    id("idea")
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
    id("xyz.wagyourtail.unimined") version "1.4.2-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

val baseName: String = property("archives_base_name") as String
val minecraftVersion: String = property("minecraft_version") as String
val supportedMinecraftVersions: String = property("supported_minecraft_versions") as String
val supportedCreateVersions: String = property("supported_create_versions") as String
val modId: String = property("mod_id") as String
val modVersion: String = property("mod_version") as String
val modName: String = property("mod_name") as String
val modDescription: String = property("mod_description") as String

version = modVersion
group = "dev.xhyrom"

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")

    project.version = rootProject.version
    project.group = rootProject.group

    repositories {
        mavenCentral()
        maven("https://maven.createmod.net")
        maven("https://maven.ithundxr.dev/snapshots")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.42")
        annotationProcessor("org.projectlombok:lombok:1.18.42")
    }

    base {
        archivesName.set("${baseName}_${name}-${modVersion}+${if (name == "neoforge") "nfg" else "fb"}_mc-${supportedMinecraftVersions}-cr-${supportedCreateVersions}")
    }

    tasks.processResources {
        inputs.property("version", modVersion)
        inputs.property("mod_name", modName)
        inputs.property("mod_description", modDescription)
        inputs.property("mod_id", modId)
        inputs.property("mod_version", modVersion)
        inputs.property("minecraft_version", minecraftVersion)
        inputs.property("supported_create_versions", supportedCreateVersions)

        filesMatching(listOf("META-INF/neoforge.mods.toml", "fabric.mod.json", "${modId}.*.mixins.json", "${modId}.mixins.json")) {
            expand(inputs.properties)
        }
    }

    if (name != "common") {
        sourceSets.main {
            output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
            resources {
                srcDirs(project(":common").sourceSets["main"].resources)
            }
        }

        tasks.named<ShadowJar>("shadowJar") {
            configurations = listOf(project.configurations.getByName("shadowBundle"))
            archiveFileName = "${base.archivesName.get()}-all.jar"
        }
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        toolchain.languageVersion = JavaLanguageVersion.of(21)
    }
}

publishMods {
    type = STABLE
    changelog = getLatestChangelog()
    version = "${modVersion}+mc-${supportedMinecraftVersions}-create${supportedCreateVersions}"

    val curseforgeToken = providers.gradleProperty("curseforge.token")
        .orElse(providers.environmentVariable("CURSEFORGE_TOKEN"))

    val modrinthToken = providers.gradleProperty("modrinth.token")
        .orElse(providers.environmentVariable("MODRINTH_TOKEN"))

    val cfOptions = curseforgeOptions {
        accessToken.set(curseforgeToken)
        projectId.set("1419839")
        minecraftVersions.add(supportedMinecraftVersions)
    }

    val mrOptions = modrinthOptions {
        accessToken.set(modrinthToken)
        projectId.set("ZmrUepY5")
        minecraftVersions.add(supportedMinecraftVersions)
    }


    curseforge("curseforgeNeoForge") {
        from(cfOptions)

        val proj = project(":neoforge")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.asJar.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} $modVersion for Minecraft $supportedMinecraftVersions and Create $supportedCreateVersions"

        modLoaders.add("neoforge")

        requires("create")
    }

    modrinth("modrinthNeoForge") {
        from(mrOptions)

        val proj = project(":neoforge")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.asJar.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} $modVersion for Minecraft $supportedMinecraftVersions and Create $supportedCreateVersions"

        modLoaders.add("neoforge")

        requires("create")
    }
}

fun getLatestChangelog(): String {
    val lines = rootProject.rootDir.resolve("CHANGELOG.md").readLines()
    val changelogLines = mutableListOf<String>()
    var inSegment = false

    for (line in lines) {
        if (line.startsWith("## ")) {
            if (inSegment) break  // next segment started, stop reading
            inSegment = true
        }
        if (inSegment) {
            changelogLines += line
        }
    }

    return changelogLines.joinToString("\n").trim()
}

tasks.register("viewLatestChangelog") {
    group = "documentation"
    description = "Print the topmost single version section from the full CHANGELOG.md file."

    doLast {
        println(getLatestChangelog())
    }
}
