package de.ggnext.railway.profile

import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

data class RailwayProfile(
    @BsonId val id: UUID,
    val name: String,
    val railwayDollars: Double,
)

data class RailwayProfileIndex(
    @BsonId val id: UUID,
    val profileIds: Set<UUID>,
)

val railwayProfileNames =
    listOf(
        "Alpha",
        "Beta",
        "Gamma",
        "Delta",
        "Omega",
        "PlayerOne",
        "Shadow",
        "Phoenix",
        "Nexus",
        "Matrix",
        "Hunter",
        "Ranger",
        "Rogue",
        "Knight",
        "Mage",
        "Nova",
        "Cosmo",
        "Vortex",
        "Titan",
        "Echo",
        "User1",
        "User2",
        "User3",
        "Guest",
        "Admin",
    )
