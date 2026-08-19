package de.ggnext.backend.friend

import de.ggnext.backend.content.BackendContent
import de.ggnext.backend.player.PlayerManager
import de.ggnext.protocol.friend.AcceptResult
import de.ggnext.protocol.friend.DenyResult
import de.ggnext.protocol.friend.Friend
import de.ggnext.protocol.friend.FriendRequestResult
import de.ggnext.protocol.friend.RemoveResult
import de.ggnext.protocol.player.FriendRequest
import java.util.UUID

class FriendSystemManager(
    private val playerManager: PlayerManager,
    private val content: BackendContent,
) {
    private val requestExpiryMillis: Long
        get() = content.getInt("numbers.velocity.friendsystem.request.expiry", 7).toLong() * 24 * 60 * 60 * 1000L

    private fun FriendRequest.isValid(): Boolean = System.currentTimeMillis() - sentAt <= requestExpiryMillis

    suspend fun getFriends(playerId: UUID): List<Friend> {
        val player = playerManager.getPlayer(playerId) ?: return emptyList()
        return player.friendIds.mapNotNull { friendId ->
            playerManager.getPlayer(friendId)?.let { Friend(it.id, it.username) }
        }
    }

    suspend fun createRequest(
        senderId: UUID,
        targetId: UUID,
    ): FriendRequestResult {
        if (senderId == targetId) return FriendRequestResult.SELF

        val sender = playerManager.getPlayer(senderId) ?: return FriendRequestResult.TARGET_NOT_FOUND
        val target = playerManager.getPlayer(targetId) ?: return FriendRequestResult.TARGET_NOT_FOUND

        if (target.friendIds.contains(senderId)) return FriendRequestResult.ALREADY_FRIENDS

        if (sender.pendingFriendRequests.any { it.from == targetId && it.isValid() }) {
            acceptRequest(senderId, targetId)
            return FriendRequestResult.ACCEPTED_MUTUAL
        }

        if (!target.settings.friendRequestsEnabled) return FriendRequestResult.REQUESTS_DISABLED

        if (target.pendingFriendRequests.any { it.from == senderId && it.isValid() }) {
            return FriendRequestResult.ALREADY_REQUESTED
        }

        val pending =
            target.pendingFriendRequests.filterNot { it.from == senderId } +
                FriendRequest(senderId, System.currentTimeMillis())
        playerManager.savePlayer(target.copy(pendingFriendRequests = pending))
        return FriendRequestResult.SENT
    }

    suspend fun acceptRequest(
        playerId: UUID,
        fromId: UUID,
    ): AcceptResult {
        val player = playerManager.getPlayer(playerId) ?: return AcceptResult.NO_REQUEST
        val request =
            player.pendingFriendRequests.firstOrNull { it.from == fromId }
                ?: return AcceptResult.NO_REQUEST

        val withoutRequest =
            player.copy(pendingFriendRequests = player.pendingFriendRequests.filterNot { it.from == fromId })

        if (!request.isValid()) {
            playerManager.savePlayer(withoutRequest)
            return AcceptResult.EXPIRED
        }

        val from = playerManager.getPlayer(fromId)
        if (from == null) {
            playerManager.savePlayer(withoutRequest)
            return AcceptResult.NO_REQUEST
        }

        playerManager.savePlayer(withoutRequest.copy(friendIds = (withoutRequest.friendIds + fromId).distinct()))
        playerManager.savePlayer(from.copy(friendIds = (from.friendIds + playerId).distinct()))
        return AcceptResult.SUCCESS
    }

    suspend fun denyRequest(
        playerId: UUID,
        fromId: UUID,
    ): DenyResult {
        val player = playerManager.getPlayer(playerId) ?: return DenyResult.NO_REQUEST
        if (player.pendingFriendRequests.none { it.from == fromId }) return DenyResult.NO_REQUEST

        playerManager.savePlayer(
            player.copy(pendingFriendRequests = player.pendingFriendRequests.filterNot { it.from == fromId }),
        )
        return DenyResult.SUCCESS
    }

    suspend fun removeFriend(
        playerId: UUID,
        friendId: UUID,
    ): RemoveResult {
        val player = playerManager.getPlayer(playerId) ?: return RemoveResult.NOT_FRIENDS
        if (!player.friendIds.contains(friendId)) return RemoveResult.NOT_FRIENDS

        playerManager.savePlayer(player.copy(friendIds = player.friendIds.filterNot { it == friendId }))
        playerManager.getPlayer(friendId)?.let { friend ->
            playerManager.savePlayer(friend.copy(friendIds = friend.friendIds.filterNot { it == playerId }))
        }
        return RemoveResult.SUCCESS
    }
}
