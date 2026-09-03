package eu.ggnext.railway.auction

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import eu.ggnext.common.db.MongoManager
import eu.ggnext.contentsystem.value.store.NumberStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.util.UUID

class AuctionManager(
    private val mongoManager: MongoManager,
) {
    private val auctionCollection =
        mongoManager.database.getCollection<AuctionItem>("auction_items")

    private val auctionExpirationDays by NumberStore("numbers.railway.auction.expiration_days")

    suspend fun createAuction(
        sellerId: UUID,
        itemStack: String,
        price: Long,
    ): AuctionItem {
        require(price > 0) { "The auction price must be greater than zero." }

        val auction =
            AuctionItem(
                sellerId = sellerId,
                itemStack = itemStack,
                price = price,
            )

        auctionCollection.insertOne(auction)
        return auction
    }

    suspend fun getAuction(auctionId: ObjectId): AuctionItem? = auctionCollection.find(Filters.eq("_id", auctionId)).firstOrNull()

    suspend fun getActiveAuctions(): List<AuctionItem> =
        auctionCollection
            .find(Filters.eq("status", AuctionStatus.ACTIVE))
            .sort(Sorts.descending("createdAt"))
            .toList()

    suspend fun buyAuction(
        auctionId: ObjectId,
        buyerId: UUID,
    ): Boolean =
        auctionCollection
            .updateOne(
                Filters.and(
                    Filters.eq("_id", auctionId),
                    Filters.eq("status", AuctionStatus.ACTIVE),
                ),
                Updates.combine(
                    Updates.set("buyerId", buyerId),
                    Updates.set("status", AuctionStatus.SOLD),
                ),
            ).modifiedCount == 1L

    suspend fun expireAuctions(currentTime: Long = System.currentTimeMillis()): Long =
        auctionCollection
            .updateMany(
                Filters.and(
                    Filters.eq("status", AuctionStatus.ACTIVE),
                    Filters.lte(
                        "createdAt",
                        currentTime - auctionExpirationDays.toLong() * MILLIS_PER_DAY,
                    ),
                ),
                Updates.set("status", AuctionStatus.EXPIRED),
            ).modifiedCount

    suspend fun getClaimableAuctions(profileId: UUID): List<AuctionItem> =
        auctionCollection
            .find(
                Filters.or(
                    Filters.and(
                        Filters.eq("status", AuctionStatus.SOLD),
                        Filters.eq("buyerId", profileId),
                    ),
                    Filters.and(
                        Filters.eq("status", AuctionStatus.EXPIRED),
                        Filters.eq("sellerId", profileId),
                    ),
                ),
            ).toList()

    suspend fun claimAuction(
        auctionId: ObjectId,
        profileId: UUID,
    ): Boolean =
        auctionCollection
            .updateOne(
                Filters.or(
                    Filters.and(
                        Filters.eq("_id", auctionId),
                        Filters.eq("status", AuctionStatus.SOLD),
                        Filters.eq("buyerId", profileId),
                    ),
                    Filters.and(
                        Filters.eq("_id", auctionId),
                        Filters.eq("status", AuctionStatus.EXPIRED),
                        Filters.eq("sellerId", profileId),
                    ),
                ),
                Updates.set("status", AuctionStatus.CLAIMED),
            ).modifiedCount == 1L

    companion object {
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}
