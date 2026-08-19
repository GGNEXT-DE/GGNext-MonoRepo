package de.ggnext.backend.verify

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.ggnext.protocol.player.Player
import de.ggnext.protocol.verify.VerifyResult
import kotlinx.coroutines.flow.firstOrNull
import org.bson.codecs.pojo.annotations.BsonId
import java.security.SecureRandom
import java.util.UUID

data class VerifyPlayer(
    @BsonId val id: UUID,
    val verifyCode: Int,
    val expiresAt: Long,
)

class VerifyManager(
    mongoDatabase: MongoDatabase,
) {
    private val collection = mongoDatabase.getCollection<VerifyPlayer>("verify_requests")
    private val players = mongoDatabase.getCollection<Player>("players")

    suspend fun createVerification(player: UUID): Pair<Int, Long> {
        val code = SecureRandom().nextInt(900000) + 100000
        val expiresAt = System.currentTimeMillis() + 5 * 60_000L
        collection.deleteOne(Filters.eq("_id", player))
        collection.insertOne(VerifyPlayer(id = player, verifyCode = code, expiresAt = expiresAt))
        return code to expiresAt
    }

    suspend fun getActive(player: UUID): VerifyPlayer? =
        collection
            .find(Filters.and(Filters.eq("_id", player), Filters.gt("expiresAt", System.currentTimeMillis())))
            .firstOrNull()

    suspend fun complete(
        code: Int,
        discordId: Long,
    ): VerifyResult {
        val request = collection.find(Filters.eq("verifyCode", code)).firstOrNull() ?: return VerifyResult.NOT_FOUND

        if (request.expiresAt <= System.currentTimeMillis()) {
            collection.deleteOne(Filters.eq("_id", request.id))
            return VerifyResult.EXPIRED
        }

        val result =
            players.updateOne(
                Filters.eq("_id", request.id),
                Updates.set("discordId", discordId),
            )
        collection.deleteOne(Filters.eq("_id", request.id))

        return if (result.matchedCount == 0L) VerifyResult.PLAYER_NOT_FOUND else VerifyResult.SUCCESS
    }
}
