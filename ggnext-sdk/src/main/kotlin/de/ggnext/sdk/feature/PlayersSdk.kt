package de.ggnext.sdk.feature

import de.ggnext.protocol.Routes
import de.ggnext.protocol.player.Player
import de.ggnext.protocol.player.PlayerActorRequest
import de.ggnext.protocol.player.PlayerGetByNameRequest
import de.ggnext.protocol.player.PlayerGetRequest
import de.ggnext.protocol.player.PlayerLoginRequest
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface PlayersSdk {
    suspend fun get(playerId: UUID): Player?

    suspend fun getByName(username: String): Player?

    suspend fun login(
        playerId: UUID,
        username: String,
    ): Player

    suspend fun savePlaytime(playerId: UUID): Boolean
}

internal class PlayersSdkImpl(
    private val ctx: SdkContext,
) : PlayersSdk {
    override suspend fun get(playerId: UUID): Player? = ctx.rpc<PlayerGetRequest, Player?>(Routes.PLAYER_GET, PlayerGetRequest(playerId))

    override suspend fun getByName(username: String): Player? =
        ctx.rpc<PlayerGetByNameRequest, Player?>(Routes.PLAYER_GET_BY_NAME, PlayerGetByNameRequest(username))

    override suspend fun login(
        playerId: UUID,
        username: String,
    ): Player = ctx.rpc(Routes.PLAYER_LOGIN, PlayerLoginRequest(playerId, username))

    override suspend fun savePlaytime(playerId: UUID): Boolean = ctx.rpc(Routes.PLAYER_SAVE_PLAYTIME, PlayerActorRequest(playerId))
}

object Players : FeatureModule<PlayersSdk> {
    override val id = FeatureId("players")

    override fun create(ctx: SdkContext): PlayersSdk = PlayersSdkImpl(ctx)
}

val GGNext.players: PlayersSdk get() = require(Players)
