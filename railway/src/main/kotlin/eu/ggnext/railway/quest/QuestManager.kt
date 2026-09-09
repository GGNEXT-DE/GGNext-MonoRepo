package eu.ggnext.railway.quest

import eu.ggnext.contentsystem.value.store.QuestStore
import eu.ggnext.contentsystem.value.types.Quest
import eu.ggnext.railway.profile.QuestProgress
import eu.ggnext.railway.profile.RailwayProfileManager
import java.util.UUID

class QuestManager(
    private val profileManager: RailwayProfileManager,
) {
    suspend fun getActiveQuests(profileId: UUID): Set<QuestProgress> {
        val profile = profileManager.getProfile(profileId) ?: return emptySet()
        return profileManager.getActiveQuests(profile)
    }

    suspend fun startQuest(
        questId: String,
        profileId: UUID,
    ) {
        val quest = getQuest(questId) ?: return
        val profile = profileManager.getProfile(profileId) ?: return
        if (profile.completedQuests.contains(questId)) return

        if (getActiveQuests(profileId).any { it.questId == questId }) return

        profileManager.addActiveQuest(profile, questId, quest.trackingType)
    }

    suspend fun updateQuest(
        questId: String,
        profileId: UUID,
    ) {
        val quest = getQuest(questId) ?: return
        val profile = profileManager.getProfile(profileId) ?: return

        val questProgress = profile.activeQuests.firstOrNull { it.questId == questId } ?: return

        questProgress.currentValue++
        if (questProgress.currentValue < quest.targetValue) {
            profileManager.updateActiveQuest(profile, questId, questProgress)
            return
        }

        profileManager.addDollars(profile, quest.rewardMoney)
        profileManager.addXp(profile, quest.rewardXp.toLong())
        profileManager.completeQuest(profile, questId)
    }

    private fun getQuest(questId: String): Quest? {
        val possibleQuests by QuestStore(questId)
        return possibleQuests.firstOrNull()
    }
}
