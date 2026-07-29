package de.ggnext.contentsystem.value.types

/**
 * [ConfigValue] wrapper for a [SkillPath]. Stored under key `"skill_path.<pathId>"`.
 */
internal data class SkillPathValue(
    override val key: String,
    override val value: SkillPath,
) : ConfigValue<SkillPath>

/**
 * A single tier within a [SkillPath]. Tiers are cumulative and stack: unlocking
 * tier N means tiers 1..N-1 are also unlocked, and all their [effects] add up.
 * There's no explicit tier number, the position in [SkillPath.tiers] is the tier number.
 *
 * @property cost Skill points needed to unlock this tier.
 * @property effects Effect values granted by this tier, e.g. "maxFuel" to 50.0.
 */
data class SkillTier(
    val cost: Int,
    val effects: Map<String, Double>,
)

/**
 * A skilltree path (e.g. "motor", "speicher"). A player's progress is a single
 * count of unlocked tiers, not individual tier flags. The next buyable tier is
 * always `tiers[unlockedCount]`, tiers can only be bought in order.
 *
 * @property id Path identifier, matches the key suffix `"skill_path.<id>"`.
 * @property tiers Ordered tiers, index 0 = tier 1.
 */
data class SkillPath(
    val id: String,
    val tiers: List<SkillTier>,
)
