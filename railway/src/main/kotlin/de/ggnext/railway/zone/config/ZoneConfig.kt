package de.ggnext.railway.zone.config

import kotlinx.serialization.Serializable
import java.io.File

data class ZoneConfig(
    val name: String,
    val schemFile: File,
    val markers: List<Marker>,
)

@Serializable
data class Marker(
    val type: ConfigPositionType,
    val x: Double,
    val y: Double,
    val z: Double,
)

enum class ConfigPositionType {
    SPAWN,
    BOSS_SPAWN,
}
