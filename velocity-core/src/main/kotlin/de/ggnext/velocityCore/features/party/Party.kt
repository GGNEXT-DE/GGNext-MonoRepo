package de.ggnext.velocityCore.features.party

import java.util.UUID

data class Party(
    val leaderId: UUID,
    val members: Set<UUID>,
    val pendingInvites: Map<UUID, Long>,
)
