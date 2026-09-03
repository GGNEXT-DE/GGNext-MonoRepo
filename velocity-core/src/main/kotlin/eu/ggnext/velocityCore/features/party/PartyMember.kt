package eu.ggnext.velocityCore.features.party

import java.util.UUID

data class PartyMember(
    val id: UUID,
    val username: String,
    val isLeader: Boolean,
)
