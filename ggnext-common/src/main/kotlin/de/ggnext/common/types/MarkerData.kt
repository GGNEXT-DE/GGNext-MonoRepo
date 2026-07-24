package de.ggnext.common.types

import kotlinx.serialization.Serializable

@Serializable
data class MarkerData(
    val type: ConfigPositionType,
    val x: Double,
    val y: Double,
    val z: Double,
)
