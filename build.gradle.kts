import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("java")
    id("idea")
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.7.+" apply false
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
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "com.github.johnrengelman.shadow")

    project.version = rootProject.version
    project.group = rootProject.group

    repositories {
        mavenCentral()
        maven("https://maven.createmod.net") // Create Forge, Ponder, Flywheel
        maven("https://maven.ithundxr.dev/mirror") // Create Forge
    }

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    dependencies {
        "minecraft"("net.minecraft:minecraft:${minecraftVersion}")
        "mappings"(
            loom.officialMojangMappings()
        )

        compileOnly("org.projectlombok:lombok:1.18.42")
        annotationProcessor("org.projectlombok:lombok:1.18.42")
    }

    base {
        archivesName.set("${baseName}_${name}-${modVersion}+mc-${supportedMinecraftVersions}-create-${supportedCreateVersions}")
    }

    tasks.processResources {
        inputs.property("version", modVersion)
        inputs.property("mod_name", modName)
        inputs.property("mod_description", modDescription)
        inputs.property("mod_id", modId)
        inputs.property("mod_version", modVersion)
        inputs.property("minecraft_version", minecraftVersion)
        inputs.property("architectury_version", rootProject.property("architectury_version"))
        inputs.property("supported_create_versions", supportedCreateVersions)

        filesMatching(listOf("META-INF/mods.toml", "fabric.mod.json", "${modId}.*.mixins.json", "${modId}.mixins.json")) {
            expand(inputs.properties)
        }
    }

    if (name != "common") {
        tasks.named<ShadowJar>("shadowJar") {
            configurations = listOf(project.configurations.getByName("shadowBundle"))
            archiveFileName = "${base.archivesName.get()}-all.jar"
        }
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        toolchain.languageVersion = JavaLanguageVersion.of(17)
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
        projectId.set("1419347")
        minecraftVersions.add(supportedMinecraftVersions)
    }

    val mrOptions = modrinthOptions {
        accessToken.set(modrinthToken)
        projectId.set("ZmrUepY5")
        minecraftVersions.add(supportedMinecraftVersions)
    }

    curseforge("curseforgeForge") {
        from(cfOptions)

        val proj = project(":forge")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} ${modVersion}+mc-${supportedMinecraftVersions}-create-${supportedCreateVersions}"

        modLoaders.add("forge")

        requires("create")
        requires("architectury-api")
    }

    curseforge("curseforgeFabric") {
        from(cfOptions)

        val proj = project(":fabric")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} ${modVersion}+mc-${supportedMinecraftVersions}-create-${supportedCreateVersions}"

        modLoaders.add("fabric")
        modLoaders.add("quilt")

        requires("fabric-api")
        requires("create")
        requires("architectury-api")
    }

    modrinth("modrinthFabric") {
        from(mrOptions)

        val proj = project(":fabric")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} ${modVersion}+mc-${supportedMinecraftVersions}-create-${supportedCreateVersions}"

        modLoaders.add("fabric")
        modLoaders.add("quilt")

        requires("fabric-api")
        requires("create")
        requires("architectury-api")
    }

    modrinth("modrinthForge") {
        from(mrOptions)

        val proj = project(":forge")
        val remapJarProvider = proj.provider {
            proj.tasks.named<RemapJarTask>("remapJar")
                .flatMap { it.archiveFile }
        }.flatMap { it }

        file.set(remapJarProvider)
        displayName = "Beamline ${proj.name.uppercaseFirstChar()} ${modVersion}+mc-${supportedMinecraftVersions}-create-${supportedCreateVersions}"

        modLoaders.add("forge")

        requires("create")
        requires("architectury-api")
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