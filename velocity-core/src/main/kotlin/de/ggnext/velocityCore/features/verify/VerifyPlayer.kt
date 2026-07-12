package de.ggnext.velocityCore.features.verify

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class VerifyPlayer(
    @BsonId val id: UUID,
    val verifyCode: Int,
    val expiresAt: Long,
)
