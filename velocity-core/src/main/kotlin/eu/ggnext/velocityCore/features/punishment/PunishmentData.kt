package eu.ggnext.velocityCore.features.punishment

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.UUID

data class PunishmentData(
    @BsonId
    val id: ObjectId = ObjectId(),
    val playerId: UUID,
    val issuedBy: UUID,
    val type: PunishmentType,
    val reason: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val revokedBy: UUID? = null,
    val revokedAt: Long? = null,
)

enum class PunishmentType {
    BAN,
    TEMP_BAN,
    MUTE,
    TEMP_MUTE,
    WARN,
}

enum class PunishmentHistoryFilter {
    ACTIVE,
    INACTIVE,
    ALL,
}
