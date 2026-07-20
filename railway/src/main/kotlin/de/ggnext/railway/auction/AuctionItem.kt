package de.ggnext.railway.auction

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.UUID

data class AuctionItem(
    @BsonId val id: ObjectId = ObjectId(),
    val sellerId: UUID,
    val itemStack: String,
    val price: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val buyerId: UUID? = null,
    val status: AuctionStatus = AuctionStatus.ACTIVE,
)

enum class AuctionStatus {
    ACTIVE,
    SOLD,
    EXPIRED,
    CLAIMED,
}
