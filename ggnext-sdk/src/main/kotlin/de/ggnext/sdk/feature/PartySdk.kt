package de.ggnext.sdk.feature

import de.ggnext.protocol.Channels
import de.ggnext.protocol.Routes
import de.ggnext.protocol.party.AcceptResult
import de.ggnext.protocol.party.DenyResult
import de.ggnext.protocol.party.DisbandResult
import de.ggnext.protocol.party.InviteResult
import de.ggnext.protocol.party.KickResult
import de.ggnext.protocol.party.LeaveResult
import de.ggnext.protocol.party.PartyActorRequest
import de.ggnext.protocol.party.PartyEvent
import de.ggnext.protocol.party.PartyInviteActionRequest
import de.ggnext.protocol.party.PartyInviteRequest
import de.ggnext.protocol.party.PartyKickRequest
import de.ggnext.protocol.party.PartyMember
import de.ggnext.protocol.party.PartyMembersResponse
import de.ggnext.sdk.EventBinding
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc
import java.util.UUID

interface PartySdk {
    suspend fun invite(
        inviter: UUID,
        target: UUID,
    ): InviteResult

    suspend fun accept(
        actor: UUID,
        leader: UUID,
    ): AcceptResult

    suspend fun deny(
        actor: UUID,
        leader: UUID,
    ): DenyResult

    suspend fun kick(
        leader: UUID,
        target: UUID,
    ): KickResult

    suspend fun leave(actor: UUID): LeaveResult

    suspend fun disband(leader: UUID): DisbandResult

    suspend fun members(actor: UUID): List<PartyMember>

    fun onInvited(handler: suspend (PartyEvent.Invited) -> Unit)

    fun onUpdated(handler: suspend (PartyEvent.Updated) -> Unit)

    fun onDisbanded(handler: suspend (PartyEvent.Disbanded) -> Unit)
}

internal class PartySdkImpl(
    private val ctx: SdkContext,
) : PartySdk {
    override suspend fun invite(
        inviter: UUID,
        target: UUID,
    ): InviteResult = ctx.rpc(Routes.PARTY_INVITE, PartyInviteRequest(inviter, target))

    override suspend fun accept(
        actor: UUID,
        leader: UUID,
    ): AcceptResult = ctx.rpc(Routes.PARTY_ACCEPT, PartyInviteActionRequest(actor, leader))

    override suspend fun deny(
        actor: UUID,
        leader: UUID,
    ): DenyResult = ctx.rpc(Routes.PARTY_DENY, PartyInviteActionRequest(actor, leader))

    override suspend fun kick(
        leader: UUID,
        target: UUID,
    ): KickResult = ctx.rpc(Routes.PARTY_KICK, PartyKickRequest(leader, target))

    override suspend fun leave(actor: UUID): LeaveResult = ctx.rpc(Routes.PARTY_LEAVE, PartyActorRequest(actor))

    override suspend fun disband(leader: UUID): DisbandResult = ctx.rpc(Routes.PARTY_DISBAND, PartyActorRequest(leader))

    override suspend fun members(actor: UUID): List<PartyMember> =
        ctx.rpc<PartyActorRequest, PartyMembersResponse>(Routes.PARTY_MEMBERS, PartyActorRequest(actor)).members

    override fun onInvited(handler: suspend (PartyEvent.Invited) -> Unit) = ctx.events.on(PartyEvent.Invited::class.java, handler)

    override fun onUpdated(handler: suspend (PartyEvent.Updated) -> Unit) = ctx.events.on(PartyEvent.Updated::class.java, handler)

    override fun onDisbanded(handler: suspend (PartyEvent.Disbanded) -> Unit) = ctx.events.on(PartyEvent.Disbanded::class.java, handler)
}

object Party : FeatureModule<PartySdk> {
    override val id = FeatureId("party")

    override fun create(ctx: SdkContext): PartySdk = PartySdkImpl(ctx)

    override fun events(): List<EventBinding> =
        listOf(
            EventBinding(Channels.EVENT_PARTY) { json, payload ->
                json.decodeFromJsonElement(PartyEvent.serializer(), payload)
            },
        )
}

val GGNext.party: PartySdk get() = require(Party)
