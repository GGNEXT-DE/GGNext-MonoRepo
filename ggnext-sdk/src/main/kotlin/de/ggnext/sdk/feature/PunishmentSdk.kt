package de.ggnext.sdk.feature

import de.ggnext.protocol.Routes
import de.ggnext.protocol.punishment.PunishmentActionRequest
import de.ggnext.protocol.punishment.PunishmentActiveRequest
import de.ggnext.protocol.punishment.PunishmentEntry
import de.ggnext.protocol.punishment.PunishmentHistoryFilter
import de.ggnext.protocol.punishment.PunishmentHistoryRequest
import de.ggnext.protocol.punishment.PunishmentHistoryResponse
import de.ggnext.protocol.punishment.PunishmentRevokeRequest
import de.ggnext.protocol.punishment.PunishmentTempRequest
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface PunishmentSdk {
    suspend fun ban(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry

    suspend fun tempBan(
        player: UUID,
        issuedBy: UUID,
        durationMillis: Long,
        reason: String,
    ): PunishmentEntry

    suspend fun mute(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry

    suspend fun tempMute(
        player: UUID,
        issuedBy: UUID,
        durationMillis: Long,
        reason: String,
    ): PunishmentEntry

    suspend fun warn(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry

    suspend fun unban(
        player: UUID,
        revokedBy: UUID,
    ): Boolean

    suspend fun unmute(
        player: UUID,
        revokedBy: UUID,
    ): Boolean

    suspend fun history(
        player: UUID,
        filter: PunishmentHistoryFilter,
    ): List<PunishmentEntry>

    suspend fun activeBan(player: UUID): PunishmentEntry?

    suspend fun activeMute(player: UUID): PunishmentEntry?
}

internal class PunishmentSdkImpl(
    private val ctx: SdkContext,
) : PunishmentSdk {
    override suspend fun ban(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry = ctx.rpc(Routes.PUNISH_BAN, PunishmentActionRequest(player, issuedBy, reason))

    override suspend fun tempBan(
        player: UUID,
        issuedBy: UUID,
        durationMillis: Long,
        reason: String,
    ): PunishmentEntry = ctx.rpc(Routes.PUNISH_TEMP_BAN, PunishmentTempRequest(player, issuedBy, durationMillis, reason))

    override suspend fun mute(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry = ctx.rpc(Routes.PUNISH_MUTE, PunishmentActionRequest(player, issuedBy, reason))

    override suspend fun tempMute(
        player: UUID,
        issuedBy: UUID,
        durationMillis: Long,
        reason: String,
    ): PunishmentEntry = ctx.rpc(Routes.PUNISH_TEMP_MUTE, PunishmentTempRequest(player, issuedBy, durationMillis, reason))

    override suspend fun warn(
        player: UUID,
        issuedBy: UUID,
        reason: String,
    ): PunishmentEntry = ctx.rpc(Routes.PUNISH_WARN, PunishmentActionRequest(player, issuedBy, reason))

    override suspend fun unban(
        player: UUID,
        revokedBy: UUID,
    ): Boolean = ctx.rpc(Routes.PUNISH_UNBAN, PunishmentRevokeRequest(player, revokedBy))

    override suspend fun unmute(
        player: UUID,
        revokedBy: UUID,
    ): Boolean = ctx.rpc(Routes.PUNISH_UNMUTE, PunishmentRevokeRequest(player, revokedBy))

    override suspend fun history(
        player: UUID,
        filter: PunishmentHistoryFilter,
    ): List<PunishmentEntry> =
        ctx
            .rpc<PunishmentHistoryRequest, PunishmentHistoryResponse>(
                Routes.PUNISH_HISTORY,
                PunishmentHistoryRequest(player, filter),
            ).entries

    override suspend fun activeBan(player: UUID): PunishmentEntry? =
        ctx.rpc<PunishmentActiveRequest, PunishmentEntry?>(Routes.PUNISH_ACTIVE_BAN, PunishmentActiveRequest(player))

    override suspend fun activeMute(player: UUID): PunishmentEntry? =
        ctx.rpc<PunishmentActiveRequest, PunishmentEntry?>(Routes.PUNISH_ACTIVE_MUTE, PunishmentActiveRequest(player))
}

object Punishment : FeatureModule<PunishmentSdk> {
    override val id = FeatureId("punishment")

    override fun create(ctx: SdkContext): PunishmentSdk = PunishmentSdkImpl(ctx)
}

val GGNext.punishment: PunishmentSdk get() = require(Punishment)
