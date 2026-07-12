package de.ggnext.core.economy

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bukkit.entity.Player

class EconomyService(
    private val database: MongoDatabase,
) {
    private val collection = database.getCollection<NetworkEconomy>("network_econemy")

    suspend fun getNetworkEconomy(player: Player): NetworkEconomy? = collection.find(Filters.eq("_id", player.uniqueId)).firstOrNull()

    suspend fun addGems(
        player: Player,
        amount: Int,
    ) {
        collection.updateOne(
            Filters.eq("_id", player.uniqueId),
            Updates.inc("gems", amount),
            UpdateOptions().upsert(true),
        )
    }

    suspend fun removeGems(
        player: Player,
        amount: Int,
    ): Boolean {
        val networkEconomy = getNetworkEconomy(player) ?: return false
        if (networkEconomy.gems < amount) return false

        collection.updateOne(
            Filters.eq("_id", player.uniqueId),
            Updates.inc("gems", -amount),
        )
        return true
    }
}
