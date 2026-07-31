package de.ggnext.core.economy

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bukkit.entity.Player
import java.util.UUID
import de.ggnext.common.economy.EconomyService as CommonEconomyService

class EconomyService(
    database: MongoDatabase,
) {
    private val delegate = CommonEconomyService(database)

    suspend fun addGems(
        player: Player,
        amount: Int,
    ) {
        delegate.addGems(player.uniqueId, amount)
    }

    suspend fun removeGems(
        player: Player,
        amount: Int,
    ): Boolean = delegate.removeGems(player.uniqueId, amount)

    suspend fun getGems(player: Player): Int = delegate.getGems(player.uniqueId)

    suspend fun addGems(
        playerId: UUID,
        amount: Int,
    ): Boolean = delegate.addGems(playerId, amount)

    suspend fun removeGems(
        playerId: UUID,
        amount: Int,
    ): Boolean = delegate.removeGems(playerId, amount)

    suspend fun getGems(playerId: UUID): Int = delegate.getGems(playerId)

    suspend fun migrateLegacyNetworkEconomy() = delegate.migrateLegacyNetworkEconomy()
}
