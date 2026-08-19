package de.ggnext.protocol.presence

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
data class PresenceUpdateRequest(
    val playerId: UuidS,
    val username: String,
    val server: String,
    val online: Boolean,
)

@Serializable
data class OnlineQuery(
    val playerIds: List<UuidS>,
)

@Serializable
data class OnlineResponse(
    val onlineIds: List<UuidS>,
)

@Serializable
data class PresenceChanged(
    val playerId: UuidS,
    val username: String,
    val server: String,
    val online: Boolean,
)
