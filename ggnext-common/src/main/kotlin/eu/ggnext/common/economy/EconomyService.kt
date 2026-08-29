package eu.ggnext.common.economy

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import eu.ggnext.common.player.Player
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class EconomyService(
    database: MongoDatabase,
) {
    private val players = database.getCollection<Player>("players")

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
}
