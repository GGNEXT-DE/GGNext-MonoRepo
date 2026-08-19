package de.ggnext.protocol.verify

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
enum class VerifyResult {
    SUCCESS,
    NOT_FOUND,
    EXPIRED,
    PLAYER_NOT_FOUND,
}

@Serializable
data class VerifyCreateRequest(
    val playerId: UuidS,
)

@Serializable
data class VerifyCreateResponse(
    val code: Int,
    val expiresAt: Long,
)

@Serializable
data class VerifyGetRequest(
    val playerId: UuidS,
)

@Serializable
data class VerifyCompleteRequest(
    val code: Int,
    val discordId: Long,
)
