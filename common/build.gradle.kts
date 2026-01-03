architectury {
    common("fabric", "forge")
}

val minecraftVersion: String = property("minecraft_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val forgeVersion: String = property("forge_version") as String
val architecturyVersion: String = property("architectury_version") as String

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    forge("net.minecraftforge:forge:${forgeVersion}")

    modCompileOnly("dev.architectury:architectury-forge:${architecturyVersion}")

    modImplementation("com.simibubi.create:create-${minecraftVersion}:${property("create_forge_version")}:slim") { isTransitive = false }
    modImplementation("net.createmod.ponder:Ponder-Forge-${minecraftVersion}:${property("ponder_version")}")
    modCompileOnly("dev.engine-room.flywheel:flywheel-forge-api-${minecraftVersion}:${property("flywheel_version")}")
    modRuntimeOnly("dev.engine-room.flywheel:flywheel-forge-${minecraftVersion}:${property("flywheel_version")}")
    modImplementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")
}