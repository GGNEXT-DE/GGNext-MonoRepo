package eu.ggnext.railway.trade

import com.mongodb.client.model.Filters
import com.noxcrew.interfaces.properties.InterfaceProperty
import eu.ggnext.common.db.MongoManager
import eu.ggnext.contentsystem.value.store.NumberStore
import eu.ggnext.core.utils.toPlayer
import eu.ggnext.railway.auction.deserializeAuctionItem
import eu.ggnext.railway.auction.serializeForAuction
import kotlinx.coroutines.flow.toList
import org.bukkit.entity.Player
import java.util.UUID

class TradeManager(
    private val mongoManager: MongoManager,
) {
    private val pendingItemCollection = mongoManager.database.getCollection<PendingTradeItem>("trade_pending_items")
    private val activeSessions = mutableMapOf<UUID, TradeSession>()
    private val activeRequests = mutableListOf<TradeRequest>()

    fun createRequest(
        sender: UUID,
        target: UUID,
    ) {
        removeRequest(sender, target)

        val expireTime by NumberStore("numbers.railway.trade.expire")

        val expirationTime = System.currentTimeMillis() + (expireTime.toInt() * 60 * 1000L)
        activeRequests.add(TradeRequest(sender, target, expirationTime))
    }

    fun removeRequest(
        sender: UUID,
        target: UUID,
    ) {
        activeRequests.removeIf { it.sender == sender && it.target == target }
    }

    fun getFirstValidRequestForTarget(target: UUID): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.target == target }
    }

    fun getValidRequestFromSender(
        sender: UUID,
        target: UUID,
    ): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.sender == sender && it.target == target }
    }

    fun cleanupRequests() {
        val now = System.currentTimeMillis()
        activeRequests.removeIf { now > it.expiration }
    }

    fun startSession(
        sender: UUID,
        target: UUID,
    ): TradeSession {
        activeRequests.removeIf { it.sender == sender && it.target == target }

        val tradePlayer1 = TradePlayer(sender, mutableListOf(), InterfaceProperty(false))
        val tradePlayer2 = TradePlayer(target, mutableListOf(), InterfaceProperty(false))
        val session = TradeSession(tradePlayer1, tradePlayer2)

        activeSessions[sender] = session
        activeSessions[target] = session

        return session
    }

    suspend fun endSession(session: TradeSession) {
        returnOffer(session.player1)
        returnOffer(session.player2)

        activeSessions.remove(session.player1.playerUUID)
        activeSessions.remove(session.player2.playerUUID)
    }

    private suspend fun returnOffer(tradePlayer: TradePlayer) {
        if (tradePlayer.offer.isEmpty()) return

        val player = tradePlayer.playerUUID.toPlayer()
        if (player != null) {
            tradePlayer.offer.forEach { player.inventory.addItem(it.clone()) }
        } else {
            tradePlayer.offer.forEach {
                pendingItemCollection.insertOne(
                    PendingTradeItem(playerId = tradePlayer.playerUUID, itemStack = it.serializeForAuction()),
                )
            }
        }
    }

    suspend fun deliverPendingItems(player: Player) {
        val pending = pendingItemCollection.find(Filters.eq("playerId", player.uniqueId)).toList()
        if (pending.isEmpty()) return

        pending.forEach { player.inventory.addItem(it.itemStack.deserializeAuctionItem()) }
        pendingItemCollection.deleteMany(Filters.eq("playerId", player.uniqueId))
    }

    fun getSession(player: UUID): TradeSession? = activeSessions[player]
}
