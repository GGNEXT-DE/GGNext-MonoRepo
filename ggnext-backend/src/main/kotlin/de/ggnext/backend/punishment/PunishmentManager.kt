package de.ggnext.backend.punishment

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.ggnext.protocol.punishment.PunishmentEntry
import de.ggnext.protocol.punishment.PunishmentHistoryFilter
import de.ggnext.protocol.punishment.PunishmentType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.UUID

data class PunishmentData(
    @BsonId val id: ObjectId = ObjectId(),
    val playerId: UUID,
    val issuedBy: UUID,
    val type: PunishmentType,
    val reason: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val revokedBy: UUID? = null,
    val revokedAt: Long? = null,
) {
    fun toEntry(): PunishmentEntry = PunishmentEntry(type, reason, issuedBy, issuedAt, expiresAt, revokedBy != null, revokedAt)
}

class PunishmentManager(
    mongoDatabase: MongoDatabase,
) {
    private val collection = mongoDatabase.getCollection<PunishmentData>("punishments")

    suspend fun getHistory(
        player: UUID,
        filter: PunishmentHistoryFilter = PunishmentHistoryFilter.ALL,
    ): List<PunishmentData> {
        val now = System.currentTimeMillis()
        val filters =
            when (filter) {
                PunishmentHistoryFilter.ACTIVE -> {
                    Filters.and(Filters.eq("playerId", player), activeFilter())
                }

                PunishmentHistoryFilter.INACTIVE -> {
                    Filters.and(
                        Filters.eq("playerId", player),
                        Filters.or(
                            Filters.eq("revokedBy", null),
                            Filters.and(
                                Filters.not(Filters.eq("revokedBy", null)),
                                Filters.ne("expiresAt", -1L),
                                Filters.lte("expiresAt", now),
                            ),
                        ),
                    )
                }

                PunishmentHistoryFilter.ALL -> {
                    Filters.eq("playerId", player)
                }
            }

        return collection.find(filters).sort(Sorts.descending("issuedAt")).toList()
    }

    suspend fun getActivePunishment(
        p: UUID,
        type: PunishmentType,
    ): PunishmentData? =
        collection
            .find(Filters.and(Filters.eq("playerId", p), Filters.eq("type", type), activeFilter()))
            .firstOrNull()

    suspend fun getActiveBan(player: UUID): PunishmentData? =
        getActivePunishment(player, PunishmentType.BAN)
            ?: getActivePunishment(player, PunishmentType.TEMP_BAN)

    suspend fun getActiveMute(player: UUID): PunishmentData? =
        getActivePunishment(player, PunishmentType.MUTE)
            ?: getActivePunishment(player, PunishmentType.TEMP_MUTE)

    suspend fun createPunishment(
        player: UUID,
        issuedBy: UUID,
        type: PunishmentType,
        reason: String,
        expiresAt: Long = -1L,
    ): PunishmentData {
        val punishment =
            PunishmentData(
                playerId = player,
                issuedBy = issuedBy,
                type = type,
                reason = reason,
                issuedAt = System.currentTimeMillis(),
                expiresAt = expiresAt,
            )
        collection.insertOne(punishment)
        return punishment
    }

    suspend fun ban(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ) = createPunishment(player, issuedBy, PunishmentType.BAN, reason)

    suspend fun tempBan(
        player: UUID,
        issuedBy: UUID,
        duration: Long,
        reason: String,
    ) = createPunishment(player, issuedBy, PunishmentType.TEMP_BAN, reason, System.currentTimeMillis() + duration)

    suspend fun mute(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ) = createPunishment(player, issuedBy, PunishmentType.MUTE, reason)

    suspend fun tempMute(
        player: UUID,
        issuedBy: UUID,
        duration: Long,
        reason: String,
    ) = createPunishment(player, issuedBy, PunishmentType.TEMP_MUTE, reason, System.currentTimeMillis() + duration)

    suspend fun warn(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ) = createPunishment(player, issuedBy, PunishmentType.WARN, reason)

    suspend fun revokeBan(
        player: UUID,
        revokedBy: UUID,
    ): Boolean {
        val p =
            getActivePunishment(player, PunishmentType.BAN)
                ?: getActivePunishment(player, PunishmentType.TEMP_BAN)
                ?: return false
        revoke(p.id, revokedBy)
        return true
    }

    suspend fun revokeMute(
        player: UUID,
        revokedBy: UUID,
    ): Boolean {
        val p =
            getActivePunishment(player, PunishmentType.MUTE)
                ?: getActivePunishment(player, PunishmentType.TEMP_MUTE)
                ?: return false
        revoke(p.id, revokedBy)
        return true
    }

    private suspend fun revoke(
        id: ObjectId,
        revokedBy: UUID,
    ) {
        collection.updateOne(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("revokedBy", revokedBy),
                Updates.set("revokedAt", System.currentTimeMillis()),
            ),
        )
    }

    private fun activeFilter(now: Long = System.currentTimeMillis()) =
        Filters.and(
            Filters.eq("revokedBy", null),
            Filters.or(Filters.eq("expiresAt", -1L), Filters.gt("expiresAt", now)),
        )
}
