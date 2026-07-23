package de.ggnext.common.player

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class Player(
    @BsonId val id: UUID,
    val username: String,
    val firstJoin: Long,
    val lastLogin: Long,
    val playTimeSeconds: Long = 0,
    val networkLevel: NetworkLevel = NetworkLevel(),
    val friendIds: List<UUID> = emptyList(),
    val pendingFriendRequests: List<FriendRequest> = emptyList(),
    val settings: PlayerSettings = PlayerSettings(),
    val discordId: Long? = null,
)

data class NetworkLevel(
    val xp: Long = 0,
    val level: Int = 1,
)

data class FriendRequest(
    val from: UUID,
    val sentAt: Long,
)

data class PlayerSettings(
    val friendRequestsEnabled: Boolean = true,
    val language: String = "de",
)
