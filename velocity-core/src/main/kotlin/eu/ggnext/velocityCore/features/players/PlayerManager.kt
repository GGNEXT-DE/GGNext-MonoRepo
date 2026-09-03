package eu.ggnext.velocityCore.features.players

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import eu.ggnext.common.player.Player
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import java.util.regex.Pattern

class PlayerManager(
    private val mongoDatabase: MongoDatabase,
) {
    private val collection = mongoDatabase.getCollection<Player>("players")

    suspend fun getPlayer(uuid: UUID): Player? = collection.find(Filters.eq("_id", uuid)).firstOrNull()

    suspend fun getPlayer(username: String): Player? =
        collection
            .find(Filters.regex("username", "^${Pattern.quote(username)}$", "i"))
            .firstOrNull()

    suspend fun savePlayer(player: Player) {
        collection.replaceOne(Filters.eq("_id", player.id), player)
    }

    suspend fun createPlayer(
        uuid: UUID,
        username: String,
    ): Player {
        val player =
            Player(
                id = uuid,
                username = username,
                firstJoin = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis(),
            )
        collection.insertOne(player)
        return player
    }

    suspend fun loginPlayer(
        uuid: UUID,
        username: String,
    ) {
        collection.updateOne(
            Filters.eq("_id", uuid),
            Updates.combine(
                Updates.set("lastLogin", System.currentTimeMillis()),
                Updates.set("username", username),
            ),
        )
    }

    suspend fun savePlaytime(uuid: UUID) {
        val player = getPlayer(uuid) ?: return
        val sessionSeconds = (System.currentTimeMillis() - player.lastLogin) / 1000

        collection.updateOne(
            Filters.eq("_id", uuid),
            Updates.inc("playTimeSeconds", sessionSeconds),
        )
    }
}
