package de.ggnext.contentsystem.value.types

internal data class SkillPathValue(
    override val key: String,
    override val value: SkillPath,
) : ConfigValue<SkillPath>

data class SkillTier(
    val cost: Int,
    val effects: Map<String, Double>,
)

data class SkillPath(
    val id: String,
    val tiers: List<SkillTier>,
)
