package de.ggnext.protocol.player

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
data class PlayerGetRequest(
    val playerId: UuidS,
)

@Serializable
data class PlayerGetByNameRequest(
    val username: String,
)

@Serializable
data class PlayerLoginRequest(
    val playerId: UuidS,
    val username: String,
)

@Serializable
data class PlayerActorRequest(
    val playerId: UuidS,
)
