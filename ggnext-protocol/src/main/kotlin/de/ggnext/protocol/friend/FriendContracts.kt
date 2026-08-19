package de.ggnext.protocol.friend

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val id: UuidS,
    val username: String,
)

@Serializable
enum class FriendRequestResult {
    SENT,
    ACCEPTED_MUTUAL,
    ALREADY_REQUESTED,
    ALREADY_FRIENDS,
    REQUESTS_DISABLED,
    SELF,
    TARGET_NOT_FOUND,
}

@Serializable
enum class AcceptResult {
    SUCCESS,
    NO_REQUEST,
    EXPIRED,
}

@Serializable
enum class DenyResult {
    SUCCESS,
    NO_REQUEST,
}

@Serializable
enum class RemoveResult {
    SUCCESS,
    NOT_FRIENDS,
}

@Serializable
data class FriendAddRequest(
    val actor: UuidS,
    val target: UuidS,
)

@Serializable
data class FriendActionRequest(
    val actor: UuidS,
    val other: UuidS,
)

@Serializable
data class FriendListRequest(
    val actor: UuidS,
)

@Serializable
data class FriendListResponse(
    val friends: List<Friend>,
)

@Serializable
sealed interface FriendEvent {
    @Serializable
    data class RequestReceived(
        val recipient: UuidS,
        val fromId: UuidS,
        val fromName: String,
    ) : FriendEvent

    @Serializable
    data class RequestAccepted(
        val recipient: UuidS,
        val byId: UuidS,
        val byName: String,
    ) : FriendEvent

    @Serializable
    data class FriendOnline(
        val recipient: UuidS,
        val friendId: UuidS,
        val friendName: String,
    ) : FriendEvent
}
