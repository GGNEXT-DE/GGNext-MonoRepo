package eu.ggnext.railway.skilltree

import eu.ggnext.contentsystem.value.store.SkillPathStore
import eu.ggnext.railway.profile.RailwayProfile
import eu.ggnext.railway.profile.RailwayProfileManager

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
        if (!canUnlock(profile, pathId)) return false

        val currentCount = profile.level.unlockedTiers[pathId] ?: 0
        return profileManager.unlockSkillTier(profile, tier.cost, pathId, currentCount + 1)
    }
}
