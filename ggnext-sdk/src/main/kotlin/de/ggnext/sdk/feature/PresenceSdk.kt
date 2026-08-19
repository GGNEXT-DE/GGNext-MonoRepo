package de.ggnext.sdk.feature

import de.ggnext.protocol.Channels
import de.ggnext.protocol.Routes
import de.ggnext.protocol.presence.OnlineQuery
import de.ggnext.protocol.presence.OnlineResponse
import de.ggnext.protocol.presence.PresenceChanged
import de.ggnext.protocol.presence.PresenceUpdateRequest
import de.ggnext.sdk.EventBinding
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface PresenceSdk {
    suspend fun update(
        playerId: UUID,
        username: String,
        server: String,
        online: Boolean,
    )

    suspend fun online(playerIds: List<UUID>): List<UUID>

    fun onPresenceChanged(handler: suspend (PresenceChanged) -> Unit)
}

internal class PresenceSdkImpl(
    private val ctx: SdkContext,
) : PresenceSdk {
    override suspend fun update(
        playerId: UUID,
        username: String,
        server: String,
        online: Boolean,
    ) {
        ctx.rpc<PresenceUpdateRequest, Boolean>(
            Routes.PRESENCE_UPDATE,
            PresenceUpdateRequest(playerId, username, server, online),
        )
    }

    override suspend fun online(playerIds: List<UUID>): List<UUID> =
        ctx.rpc<OnlineQuery, OnlineResponse>(Routes.PRESENCE_ONLINE, OnlineQuery(playerIds)).onlineIds

    override fun onPresenceChanged(handler: suspend (PresenceChanged) -> Unit) = ctx.events.on(PresenceChanged::class.java, handler)
}

object Presence : FeatureModule<PresenceSdk> {
    override val id = FeatureId("presence")

    override fun create(ctx: SdkContext): PresenceSdk = PresenceSdkImpl(ctx)

    override fun events(): List<EventBinding> =
        listOf(
            EventBinding(Channels.EVENT_PRESENCE) { json, payload ->
                json.decodeFromJsonElement(PresenceChanged.serializer(), payload)
            },
        )
}

val GGNext.presence: PresenceSdk get() = require(Presence)
