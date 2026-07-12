package de.ggnext.railway.profile

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class RailwayProfile(
    @BsonId val id: UUID,
    val name: String,
    val createdAt: Long,
    val lastPlayed: Long,
    val railwayDollars: Double,
)

data class RailwayProfileIndex(
    @BsonId val id: UUID,
    val profileIds: Set<UUID>,
)
