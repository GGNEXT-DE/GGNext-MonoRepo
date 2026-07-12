package de.ggnext.railway.zone.session

import java.util.UUID

data class ZoneSession(
    val playerUUID: UUID,
    val profileUUID: UUID,
    val zoneId: String,
    val slot: Int,
)
