package eu.ggnext.railway.trade

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.UUID

data class PendingTradeItem(
    @BsonId val id: ObjectId = ObjectId(),
    val playerId: UUID,
    val itemStack: String,
    val createdAt: Long = System.currentTimeMillis(),
)
