package de.ggnext.protocol.punishment

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
enum class PunishmentType {
    BAN,
    TEMP_BAN,
    MUTE,
    TEMP_MUTE,
    WARN,
}

@Serializable
enum class PunishmentHistoryFilter {
    ACTIVE,
    INACTIVE,
    ALL,
}

@Serializable
data class PunishmentEntry(
    val type: PunishmentType,
    val reason: String,
    val issuedBy: UuidS,
    val issuedAt: Long,
    val expiresAt: Long,
    val revoked: Boolean,
    val revokedAt: Long? = null,
)

@Serializable
data class PunishmentActionRequest(
    val player: UuidS,
    val issuedBy: UuidS,
    val reason: String,
)

@Serializable
data class PunishmentTempRequest(
    val player: UuidS,
    val issuedBy: UuidS,
    val durationMillis: Long,
    val reason: String,
)

@Serializable
data class PunishmentRevokeRequest(
    val player: UuidS,
    val revokedBy: UuidS,
)

@Serializable
data class PunishmentHistoryRequest(
    val player: UuidS,
    val filter: PunishmentHistoryFilter,
)

@Serializable
data class PunishmentHistoryResponse(
    val entries: List<PunishmentEntry>,
)

@Serializable
data class PunishmentActiveRequest(
    val player: UuidS,
)
