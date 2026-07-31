package de.ggnext.common.economy

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.ggnext.common.player.Player
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import java.util.UUID

class EconomyService(
    database: MongoDatabase,
) {
    private val players = database.getCollection<Player>("players")
    private val legacyNetworkEconomy = database.getCollection<Document>("network_econemy")

    suspend fun getGems(playerId: UUID): Int = players.find(Filters.eq("_id", playerId)).firstOrNull()?.gems ?: 0

    suspend fun addGems(
        playerId: UUID,
        amount: Int,
    ): Boolean {
        if (amount <= 0) return false

        val result =
            players.updateOne(
                Filters.eq("_id", playerId),
                Updates.inc("gems", amount),
            )

        return result.modifiedCount > 0
    }

    suspend fun removeGems(
        playerId: UUID,
        amount: Int,
    ): Boolean {
        if (amount <= 0) return false

        val result =
            players.updateOne(
                Filters.and(
                    Filters.eq("_id", playerId),
                    Filters.gte("gems", amount),
                ),
                Updates.inc("gems", -amount),
            )

        return result.modifiedCount > 0
    }

    suspend fun migrateLegacyNetworkEconomy() {
        legacyNetworkEconomy.find().collect { legacyEconomy ->
            val playerId = legacyEconomy.get("_id", UUID::class.java) ?: return@collect
            val gems = legacyEconomy.getInteger("gems") ?: return@collect

            players.updateOne(
                Filters.and(
                    Filters.eq("_id", playerId),
                    Filters.exists("gems", false),
                ),
                Updates.set("gems", gems),
            )
        }
    }
}
