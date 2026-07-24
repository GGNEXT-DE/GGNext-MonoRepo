package de.ggnext.railway.zone.config

import de.ggnext.common.types.MarkerData
import java.io.File

data class ZoneConfig(
    val name: String,
    val schemFile: File,
    val markers: List<MarkerData>,
)
