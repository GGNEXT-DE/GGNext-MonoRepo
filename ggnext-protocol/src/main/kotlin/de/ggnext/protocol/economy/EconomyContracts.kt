package de.ggnext.protocol.economy

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
data class EconomyGetRequest(
    val playerId: UuidS,
)

@Serializable
data class EconomyChangeRequest(
    val playerId: UuidS,
    val amount: Int,
)
