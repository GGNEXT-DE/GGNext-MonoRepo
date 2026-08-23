package de.ggnext.railway.skilltree.effect

import de.ggnext.contentsystem.value.types.EffectType
import de.ggnext.contentsystem.value.types.SkillPath

class EffectManager {
    fun calculateEffects(
        paths: List<SkillPath>,
        unlockedCounts: Map<String, Int>,
    ): Map<EffectType, Double> {
        val result = mutableMapOf<EffectType, Double>()
        for ((id, tiers) in paths) {
            val unlocked = unlockedCounts[id] ?: 0
            tiers.take(unlocked).forEach { tier ->
                tier.effects.forEach { (type, value) ->
                    result.merge(type, value, Double::plus)
                }
            }
        }
        return result
    }
}
