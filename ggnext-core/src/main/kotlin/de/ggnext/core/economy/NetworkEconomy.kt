package de.ggnext.core.economy

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class NetworkEconomy(
    @BsonId val id: UUID,
    val gems: Int,
)
