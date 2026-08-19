package de.ggnext.sdk.feature

import de.ggnext.protocol.Routes
import de.ggnext.protocol.verify.VerifyCompleteRequest
import de.ggnext.protocol.verify.VerifyCreateRequest
import de.ggnext.protocol.verify.VerifyCreateResponse
import de.ggnext.protocol.verify.VerifyGetRequest
import de.ggnext.protocol.verify.VerifyResult
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface VerifySdk {
    suspend fun create(playerId: UUID): VerifyCreateResponse

    suspend fun getActive(playerId: UUID): VerifyCreateResponse?

    suspend fun complete(
        code: Int,
        discordId: Long,
    ): VerifyResult
}

internal class VerifySdkImpl(
    private val ctx: SdkContext,
) : VerifySdk {
    override suspend fun create(playerId: UUID): VerifyCreateResponse = ctx.rpc(Routes.VERIFY_CREATE, VerifyCreateRequest(playerId))

    override suspend fun getActive(playerId: UUID): VerifyCreateResponse? =
        ctx.rpc<VerifyGetRequest, VerifyCreateResponse?>(Routes.VERIFY_GET, VerifyGetRequest(playerId))

    override suspend fun complete(
        code: Int,
        discordId: Long,
    ): VerifyResult = ctx.rpc(Routes.VERIFY_COMPLETE, VerifyCompleteRequest(code, discordId))
}

object Verify : FeatureModule<VerifySdk> {
    override val id = FeatureId("verify")

    override fun create(ctx: SdkContext): VerifySdk = VerifySdkImpl(ctx)
}

val GGNext.verify: VerifySdk get() = require(Verify)
