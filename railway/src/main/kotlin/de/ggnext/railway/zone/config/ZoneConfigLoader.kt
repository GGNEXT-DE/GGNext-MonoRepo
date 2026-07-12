package de.ggnext.railway.zone.config

import kotlinx.serialization.json.Json
import org.bukkit.plugin.java.JavaPlugin

class ZoneConfigLoader(
    private val plugin: JavaPlugin,
) {
    fun load(): List<ZoneConfig> {
        val parentDir = plugin.dataFolder.resolve("zones")

        if (!parentDir.exists() || !parentDir.isDirectory) {
            parentDir.mkdirs()
            return emptyList()
        }

        val directories = parentDir.listFiles() ?: return emptyList()

        val zoneConfigs = mutableListOf<ZoneConfig>()

        directories.forEach { directory ->
            if (!directory.isDirectory) return@forEach

            val name = directory.name

            val schemFile = directory.resolve("build.schem")
            if (!schemFile.exists() || !schemFile.isFile) return@forEach

            val jsonFile = directory.resolve("markers.json")
            if (!jsonFile.exists() || !jsonFile.isFile) return@forEach

            val markers = Json.decodeFromString<List<Marker>>(jsonFile.readText())

            zoneConfigs.add(ZoneConfig(name, schemFile, markers))
        }
        return zoneConfigs
    }
}
