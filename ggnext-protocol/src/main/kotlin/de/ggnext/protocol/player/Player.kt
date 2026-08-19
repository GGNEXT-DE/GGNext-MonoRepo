package de.ggnext.protocol.player

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class Player(
    @BsonId val id: UuidS,
    val username: String,
    val firstJoin: Long,
    val lastLogin: Long,
    val playTimeSeconds: Long = 0,
    val gems: Int = 0,
    val networkLevel: NetworkLevel = NetworkLevel(),
    val friendIds: List<UuidS> = emptyList(),
    val pendingFriendRequests: List<FriendRequest> = emptyList(),
    val settings: PlayerSettings = PlayerSettings(),
    val discordId: Long? = null,
)

@Serializable
data class NetworkLevel(
    val xp: Long = 0,
    val level: Int = 1,
)

@Serializable
data class FriendRequest(
    val from: UuidS,
    val sentAt: Long,
)

@Serializable
data class PlayerSettings(
    val friendRequestsEnabled: Boolean = true,
    val language: String = "de",
)
