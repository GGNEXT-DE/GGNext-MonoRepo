package de.ggnext.sdk.feature

import de.ggnext.protocol.Channels
import de.ggnext.protocol.Routes
import de.ggnext.protocol.friend.AcceptResult
import de.ggnext.protocol.friend.DenyResult
import de.ggnext.protocol.friend.Friend
import de.ggnext.protocol.friend.FriendActionRequest
import de.ggnext.protocol.friend.FriendAddRequest
import de.ggnext.protocol.friend.FriendEvent
import de.ggnext.protocol.friend.FriendListRequest
import de.ggnext.protocol.friend.FriendListResponse
import de.ggnext.protocol.friend.FriendRequestResult
import de.ggnext.protocol.friend.RemoveResult
import de.ggnext.sdk.EventBinding
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface FriendsSdk {
    suspend fun add(
        actor: UUID,
        target: UUID,
    ): FriendRequestResult

    suspend fun accept(
        actor: UUID,
        from: UUID,
    ): AcceptResult

    suspend fun deny(
        actor: UUID,
        from: UUID,
    ): DenyResult

    suspend fun remove(
        actor: UUID,
        friend: UUID,
    ): RemoveResult

    suspend fun list(actor: UUID): List<Friend>

    fun onRequestReceived(handler: suspend (FriendEvent.RequestReceived) -> Unit)

    fun onRequestAccepted(handler: suspend (FriendEvent.RequestAccepted) -> Unit)

    fun onFriendOnline(handler: suspend (FriendEvent.FriendOnline) -> Unit)
}

internal class FriendsSdkImpl(
    private val ctx: SdkContext,
) : FriendsSdk {
    override suspend fun add(
        actor: UUID,
        target: UUID,
    ): FriendRequestResult = ctx.rpc(Routes.FRIEND_ADD, FriendAddRequest(actor, target))

    override suspend fun accept(
        actor: UUID,
        from: UUID,
    ): AcceptResult = ctx.rpc(Routes.FRIEND_ACCEPT, FriendActionRequest(actor, from))

    override suspend fun deny(
        actor: UUID,
        from: UUID,
    ): DenyResult = ctx.rpc(Routes.FRIEND_DENY, FriendActionRequest(actor, from))

    override suspend fun remove(
        actor: UUID,
        friend: UUID,
    ): RemoveResult = ctx.rpc(Routes.FRIEND_REMOVE, FriendActionRequest(actor, friend))

    override suspend fun list(actor: UUID): List<Friend> =
        ctx.rpc<FriendListRequest, FriendListResponse>(Routes.FRIEND_LIST, FriendListRequest(actor)).friends

    override fun onRequestReceived(handler: suspend (FriendEvent.RequestReceived) -> Unit) =
        ctx.events.on(FriendEvent.RequestReceived::class.java, handler)

    override fun onRequestAccepted(handler: suspend (FriendEvent.RequestAccepted) -> Unit) =
        ctx.events.on(FriendEvent.RequestAccepted::class.java, handler)

    override fun onFriendOnline(handler: suspend (FriendEvent.FriendOnline) -> Unit) =
        ctx.events.on(FriendEvent.FriendOnline::class.java, handler)
}

object Friends : FeatureModule<FriendsSdk> {
    override val id = FeatureId("friends")

    override fun create(ctx: SdkContext): FriendsSdk = FriendsSdkImpl(ctx)

    override fun events(): List<EventBinding> =
        listOf(
            EventBinding(Channels.EVENT_FRIEND) { json, payload ->
                json.decodeFromJsonElement(FriendEvent.serializer(), payload)
            },
        )
}

val GGNext.friends: FriendsSdk get() = require(Friends)
