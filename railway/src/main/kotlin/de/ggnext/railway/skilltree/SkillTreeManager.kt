package de.ggnext.railway.skilltree

import de.ggnext.contentsystem.value.store.SkillPathStore
import de.ggnext.railway.profile.RailwayProfile
import de.ggnext.railway.profile.RailwayProfileManager

class SkillTreeManager(
    private val profileManager: RailwayProfileManager,
) {
    private fun nextTier(
        profile: RailwayProfile,
        pathId: String,
    ) = run {
        val skillPath by SkillPathStore("skill_path.$pathId")
        val unlockedCount = profile.level.unlockedTiers[pathId] ?: 0
        skillPath.tiers.getOrNull(unlockedCount)
    }

    fun canUnlock(
        profile: RailwayProfile,
        pathId: String,
    ): Boolean {
        val tier = nextTier(profile, pathId) ?: return false
        return profile.level.skillPoints >= tier.cost
    }

    suspend fun unlock(
        profile: RailwayProfile,
        pathId: String,
    ): Boolean {
        val tier = nextTier(profile, pathId) ?: return false
        if (!canUnlock()) return false

        val currentCount = profile.level.unlockedTiers[pathId] ?: 0
        val updatedLevel =
            profile.level.copy(
                skillPoints = profile.level.skillPoints - tier.cost,
                unlockedTiers = profile.level.unlockedTiers + (pathId to currentCount + 1),
            )
        profileManager.updateProfile(profile.copy(level = updatedLevel))
        return true
    }
}
