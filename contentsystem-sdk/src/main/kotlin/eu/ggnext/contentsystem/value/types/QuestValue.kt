package eu.ggnext.contentsystem.value.types

enum class QuestCategory {
    DAILY,
    WEEKLY,
    MILESTONE,
}

enum class QuestTrackingType {
    ZONE_VISITED,
    ITEM_MINED,
    ITEM_CRAFTED,
    BLOCKS_WALKED,
}

internal data class QuestValue(
    override val key: String,
    override val value: Quest,
) : ConfigValue<Quest>

data class Quest(
    val name: Translation,
    val description: Translation,
    val category: QuestCategory,
    val trackingType: QuestTrackingType,
    val trackingTarget: String?,
    val targetValue: Int,
    val rewardXp: Int,
    val rewardMoney: Double,
)
