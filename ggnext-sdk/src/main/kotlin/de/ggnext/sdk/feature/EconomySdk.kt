package de.ggnext.sdk.feature

import de.ggnext.protocol.Routes
import de.ggnext.protocol.economy.EconomyChangeRequest
import de.ggnext.protocol.economy.EconomyGetRequest
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface EconomySdk {
    suspend fun getGems(playerId: UUID): Int

    suspend fun addGems(
        playerId: UUID,
        amount: Int,
    ): Boolean

    suspend fun removeGems(
        playerId: UUID,
        amount: Int,
    ): Boolean
}

internal class EconomySdkImpl(
    private val ctx: SdkContext,
) : EconomySdk {
    override suspend fun getGems(playerId: UUID): Int = ctx.rpc(Routes.ECONOMY_GET, EconomyGetRequest(playerId))

    override suspend fun addGems(
        playerId: UUID,
        amount: Int,
    ): Boolean = ctx.rpc(Routes.ECONOMY_ADD, EconomyChangeRequest(playerId, amount))

    override suspend fun removeGems(
        playerId: UUID,
        amount: Int,
    ): Boolean = ctx.rpc(Routes.ECONOMY_REMOVE, EconomyChangeRequest(playerId, amount))
}

object Economy : FeatureModule<EconomySdk> {
    override val id = FeatureId("economy")

    override fun create(ctx: SdkContext): EconomySdk = EconomySdkImpl(ctx)
}

val GGNext.economy: EconomySdk get() = require(Economy)
