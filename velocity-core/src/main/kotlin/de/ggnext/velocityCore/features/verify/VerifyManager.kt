package de.ggnext.velocityCore.features.verify

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.security.SecureRandom
import java.util.UUID

class VerifyManager(
    private val mongoDatabase: MongoDatabase,
) {
    val collection = mongoDatabase.getCollection<VerifyPlayer>("verify_requests")

    suspend fun createVerification(player: UUID): Int {
        val code = SecureRandom().nextInt(900000) + 100000
        val verify =
            VerifyPlayer(
                id = player,
                verifyCode = code,
                expiresAt = System.currentTimeMillis() + 5 * 60_000L,
            )
        if (getVerificationProcess(player) != null) {
            deleteVerificationProcess(player)
        }
        collection.insertOne(verify)
        return code
    }

    suspend fun deleteVerificationProcess(player: UUID) {
        collection.deleteOne(Filters.eq("_id", player))
    }

    suspend fun getVerificationProcess(player: UUID): VerifyPlayer? = collection.find(Filters.eq("_id", player)).firstOrNull()

    suspend fun getActiveVerificationProcess(player: UUID): VerifyPlayer? =
        collection
            .find(
                Filters.and(
                    Filters.eq("_id", player),
                    Filters.gt("expiresAt", System.currentTimeMillis()),
                ),
            ).firstOrNull()
}
