package eu.ggnext.railway.profile

import eu.ggnext.contentsystem.value.types.QuestTrackingType
import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID
import kotlin.math.pow

data class RailwayProfile(
    @BsonId val id: UUID,
    val name: String,
    val railwayDollars: Double,
    val level: RailwayLevel,
    val completedQuests: Set<String>,
    val activeQuests: Set<QuestProgress>,
)

data class RailwayProfileIndex(
    @BsonId val id: UUID,
    val profileIds: Set<UUID>,
)

data class RailwayLevel(
    val xp: Long,
    val skillPoints: Int,
    val unlockedTiers: Map<String, Int> = emptyMap(),
) {
    fun neededXpForLevel(level: Int): Long = (100 * level.toDouble().pow(2)).toLong()

    val currentLevel: Int
        get() {
            var level = 0
            while (xp >= neededXpForLevel(level + 1)) {
                level++
            }
            return level
        }
}

data class QuestProgress(
    val questId: String,
    val type: QuestTrackingType,
    val currentValue: Int = 0,
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
