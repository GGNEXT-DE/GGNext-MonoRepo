package de.ggnext.velocityCore.features.party

import de.ggnext.contentsystem.value.store.NumberStore
import de.ggnext.velocityCore.features.party.results.AcceptResult
import de.ggnext.velocityCore.features.party.results.DenyResult
import de.ggnext.velocityCore.features.party.results.DisbandResult
import de.ggnext.velocityCore.features.party.results.InviteResult
import de.ggnext.velocityCore.features.party.results.KickResult
import de.ggnext.velocityCore.features.party.results.LeaveResult
import de.ggnext.velocityCore.features.players.PlayerManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class PartySystemManager(
    private val playerManager: PlayerManager,
) {
    private val maxPartySize by NumberStore("numbers.velocity.partysystem.max_size")

    private val inviteExpiryMinutes by NumberStore("numbers.velocity.partysystem.invite.expiry")

    private val inviteExpiryMillis: Long
        get() = inviteExpiryMinutes.toLong() * 60 * 1000L

    private val mutex = Mutex()

    private val parties = mutableMapOf<UUID, Party>()
    private val memberIndex = mutableMapOf<UUID, UUID>()

    private fun partyOf(playerId: UUID): Party? = memberIndex[playerId]?.let { parties[it] }

    private fun isExpired(sentAt: Long): Boolean = System.currentTimeMillis() - sentAt > inviteExpiryMillis

    private fun createParty(leaderId: UUID): Party {
        val party = Party(leaderId = leaderId, members = setOf(leaderId), pendingInvites = emptyMap())
        parties[leaderId] = party
        memberIndex[leaderId] = leaderId
        return party
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
                return@withLock AcceptResult.EXPIRED
            }

            if (withoutInvite.members.size >= maxPartySize) {
                parties[leaderId] = withoutInvite
                return@withLock AcceptResult.PARTY_FULL
            }

            parties[leaderId] = withoutInvite.copy(members = withoutInvite.members + playerId)
            memberIndex[playerId] = leaderId
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

            parties[leaderId] = party.copy(members = party.members - targetId)
            memberIndex.remove(targetId)
            KickResult.SUCCESS
        }

    suspend fun leaveParty(playerId: UUID): LeaveResult =
        mutex.withLock {
            val leaderId = memberIndex[playerId] ?: return@withLock LeaveResult.NOT_IN_PARTY
            val party = parties[leaderId] ?: return@withLock LeaveResult.NOT_IN_PARTY

            if (party.leaderId == playerId) {
                party.members.forEach(memberIndex::remove)
                parties.remove(leaderId)
                return@withLock LeaveResult.DISBANDED
            }

            parties[leaderId] = party.copy(members = party.members - playerId)
            memberIndex.remove(playerId)
            LeaveResult.LEFT
        }

    suspend fun disbandParty(leaderId: UUID): DisbandResult =
        mutex.withLock {
            val party = parties[leaderId] ?: return@withLock DisbandResult.NOT_LEADER

            party.members.forEach(memberIndex::remove)
            parties.remove(leaderId)
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
