package de.ggnext.protocol.party

import de.ggnext.protocol.UuidS
import kotlinx.serialization.Serializable

@Serializable
data class PartyMember(
    val id: UuidS,
    val username: String,
    val isLeader: Boolean,
)

@Serializable
enum class InviteResult {
    SENT,
    NOT_LEADER,
    ALREADY_INVITED,
    TARGET_IN_PARTY,
    PARTY_FULL,
    SELF,
}

@Serializable
enum class AcceptResult {
    SUCCESS,
    NO_INVITE,
    EXPIRED,
    PARTY_FULL,
    ALREADY_IN_PARTY,
}

@Serializable
enum class DenyResult {
    SUCCESS,
    NO_INVITE,
}

@Serializable
enum class KickResult {
    SUCCESS,
    NOT_LEADER,
    NOT_MEMBER,
    CANNOT_KICK_SELF,
}

@Serializable
enum class LeaveResult {
    LEFT,
    DISBANDED,
    NOT_IN_PARTY,
}

@Serializable
enum class DisbandResult {
    SUCCESS,
    NOT_LEADER,
}

@Serializable
data class PartyInviteRequest(
    val inviter: UuidS,
    val target: UuidS,
)

@Serializable
data class PartyInviteActionRequest(
    val actor: UuidS,
    val leader: UuidS,
)

@Serializable
data class PartyKickRequest(
    val leader: UuidS,
    val target: UuidS,
)

@Serializable
data class PartyActorRequest(
    val actor: UuidS,
)

@Serializable
data class PartyMembersResponse(
    val members: List<PartyMember>,
)

@Serializable
sealed interface PartyEvent {
    @Serializable
    data class Invited(
        val target: UuidS,
        val leaderId: UuidS,
        val leaderName: String,
    ) : PartyEvent

    @Serializable
    data class Updated(
        val memberIds: List<UuidS>,
        val leaderId: UuidS,
        val reason: String,
    ) : PartyEvent

    @Serializable
    data class Disbanded(
        val memberIds: List<UuidS>,
        val leaderId: UuidS,
    ) : PartyEvent
}
