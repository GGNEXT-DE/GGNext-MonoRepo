package de.ggnext.backend.party

import de.ggnext.backend.content.BackendContent
import de.ggnext.backend.player.PlayerManager
import de.ggnext.protocol.Channels
import de.ggnext.protocol.UuidS
import de.ggnext.protocol.party.AcceptResult
import de.ggnext.protocol.party.DenyResult
import de.ggnext.protocol.party.DisbandResult
import de.ggnext.protocol.party.InviteResult
import de.ggnext.protocol.party.KickResult
import de.ggnext.protocol.party.LeaveResult
import de.ggnext.protocol.party.PartyEvent
import de.ggnext.protocol.party.PartyMember
import de.ggnext.transport.EventBus
import de.ggnext.transport.RedisTransport
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
internal data class PartyState(
    val leaderId: UuidS,
    val members: Set<UuidS>,
    val pendingInvites: Map<UuidS, Long>,
)

@Serializable
internal data class PartiesSnapshot(
    val parties: List<PartyState>,
)

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PartyManager(
    private val playerManager: PlayerManager,
    private val eventBus: EventBus,
    private val json: Json,
    private val content: BackendContent,
    transport: RedisTransport,
) {
    private val stateKey = "ggnext:party:state"
    private val commands = transport.commands.coroutines()
    private val mutex = Mutex()

    private val parties = mutableMapOf<UUID, PartyState>()
    private val memberIndex = mutableMapOf<UUID, UUID>()

    private val maxPartySize: Int get() = content.getInt("numbers.velocity.partysystem.max_size", 4)

    private val inviteExpiryMillis: Long
        get() = content.getInt("numbers.velocity.partysystem.invite.expiry", 5).toLong() * 60 * 1000L

    private fun partyOf(playerId: UUID): PartyState? = memberIndex[playerId]?.let { parties[it] }

    private fun isExpired(sentAt: Long): Boolean = System.currentTimeMillis() - sentAt > inviteExpiryMillis

    private fun createParty(leaderId: UUID): PartyState {
        val party = PartyState(leaderId = leaderId, members = setOf(leaderId), pendingInvites = emptyMap())
        parties[leaderId] = party
        memberIndex[leaderId] = leaderId
        return party
    }

    suspend fun load() {
        val raw = commands.get(stateKey) ?: return
        val snapshot = runCatching { json.decodeFromString(PartiesSnapshot.serializer(), raw) }.getOrNull() ?: return
        parties.clear()
        memberIndex.clear()
        for (party in snapshot.parties) {
            parties[party.leaderId] = party
            party.members.forEach { memberIndex[it] = party.leaderId }
        }
    }

    private suspend fun persist() {
        val snapshot = PartiesSnapshot(parties.values.toList())
        commands.set(stateKey, json.encodeToString(PartiesSnapshot.serializer(), snapshot))
    }

    private suspend fun publish(event: PartyEvent) {
        eventBus.publish(Channels.EVENT_PARTY, json.encodeToJsonElement(PartyEvent.serializer(), event))
    }

    suspend fun invitePlayer(
        inviterId: UUID,
        targetId: UUID,
    ): InviteResult =
        mutex.withLock {
            if (inviterId == targetId) return@withLock InviteResult.SELF

            val party =
                partyOf(inviterId)?.also {
                    if (it.leaderId != inviterId) return@withLock InviteResult.NOT_LEADER
                } ?: createParty(inviterId)

            if (partyOf(targetId) != null) return@withLock InviteResult.TARGET_IN_PARTY

            if (party.members.size >= maxPartySize) return@withLock InviteResult.PARTY_FULL

            val alreadyInvited = party.pendingInvites[targetId]?.let { !isExpired(it) } ?: false
            if (alreadyInvited) return@withLock InviteResult.ALREADY_INVITED

            parties[party.leaderId] =
                party.copy(pendingInvites = party.pendingInvites + (targetId to System.currentTimeMillis()))
            persist()

            val leaderName = playerManager.getPlayer(party.leaderId)?.username ?: "?"
            publish(PartyEvent.Invited(targetId, party.leaderId, leaderName))
            InviteResult.SENT
        }

    suspend fun acceptInvite(
        playerId: UUID,
        leaderId: UUID,
    ): AcceptResult =
        mutex.withLock {
            if (partyOf(playerId) != null) return@withLock AcceptResult.ALREADY_IN_PARTY

            val party = parties[leaderId] ?: return@withLock AcceptResult.NO_INVITE
            val sentAt = party.pendingInvites[playerId] ?: return@withLock AcceptResult.NO_INVITE

            val withoutInvite = party.copy(pendingInvites = party.pendingInvites - playerId)

            if (isExpired(sentAt)) {
                parties[leaderId] = withoutInvite
                persist()
                return@withLock AcceptResult.EXPIRED
            }

            if (withoutInvite.members.size >= maxPartySize) {
                parties[leaderId] = withoutInvite
                persist()
                return@withLock AcceptResult.PARTY_FULL
            }

            val updated = withoutInvite.copy(members = withoutInvite.members + playerId)
            parties[leaderId] = updated
            memberIndex[playerId] = leaderId
            persist()
            publish(PartyEvent.Updated(updated.members.toList(), leaderId, "join"))
            AcceptResult.SUCCESS
        }

    suspend fun denyInvite(
        playerId: UUID,
        leaderId: UUID,
    ): DenyResult =
        mutex.withLock {
            val party = parties[leaderId] ?: return@withLock DenyResult.NO_INVITE
            if (!party.pendingInvites.containsKey(playerId)) return@withLock DenyResult.NO_INVITE

            parties[leaderId] = party.copy(pendingInvites = party.pendingInvites - playerId)
            persist()
            DenyResult.SUCCESS
        }

    suspend fun kickPlayer(
        leaderId: UUID,
        targetId: UUID,
    ): KickResult =
        mutex.withLock {
            if (targetId == leaderId) return@withLock KickResult.CANNOT_KICK_SELF

            val party = parties[leaderId] ?: return@withLock KickResult.NOT_LEADER
            if (!party.members.contains(targetId)) return@withLock KickResult.NOT_MEMBER

            val updated = party.copy(members = party.members - targetId)
            parties[leaderId] = updated
            memberIndex.remove(targetId)
            persist()
            publish(PartyEvent.Updated(updated.members.toList(), leaderId, "kick"))
            KickResult.SUCCESS
        }

    suspend fun leaveParty(playerId: UUID): LeaveResult =
        mutex.withLock {
            val leaderId = memberIndex[playerId] ?: return@withLock LeaveResult.NOT_IN_PARTY
            val party = parties[leaderId] ?: return@withLock LeaveResult.NOT_IN_PARTY

            if (party.leaderId == playerId) {
                party.members.forEach(memberIndex::remove)
                parties.remove(leaderId)
                persist()
                publish(PartyEvent.Disbanded(party.members.toList(), leaderId))
                return@withLock LeaveResult.DISBANDED
            }

            val updated = party.copy(members = party.members - playerId)
            parties[leaderId] = updated
            memberIndex.remove(playerId)
            persist()
            publish(PartyEvent.Updated(updated.members.toList(), leaderId, "leave"))
            LeaveResult.LEFT
        }

    suspend fun disbandParty(leaderId: UUID): DisbandResult =
        mutex.withLock {
            val party = parties[leaderId] ?: return@withLock DisbandResult.NOT_LEADER

            party.members.forEach(memberIndex::remove)
            parties.remove(leaderId)
            persist()
            publish(PartyEvent.Disbanded(party.members.toList(), leaderId))
            DisbandResult.SUCCESS
        }

    suspend fun getPartyMembers(playerId: UUID): List<PartyMember> {
        val party =
            mutex.withLock {
                val leaderId = memberIndex[playerId] ?: return emptyList()
                parties[leaderId]
            } ?: return emptyList()

        return party.members.mapNotNull { id ->
            playerManager.getPlayer(id)?.let { PartyMember(it.id, it.username, id == party.leaderId) }
        }
    }
}
