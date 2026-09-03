package eu.ggnext.railway.quest

import eu.ggnext.contentsystem.value.store.QuestStore
import eu.ggnext.contentsystem.value.types.Quest
import eu.ggnext.railway.profile.RailwayProfileManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class QuestManager(
    private val profileManager: RailwayProfileManager,
) {
    private val activeQuests = ConcurrentHashMap<UUID, MutableList<QuestProgress>>()

    fun getActiveQuests(profileId: UUID): List<QuestProgress> =
        activeQuests[profileId]?.let { list -> synchronized(list) { list.toList() } } ?: emptyList()

    suspend fun startQuest(
        questId: String,
        profileId: UUID,
    ) {
        getQuest(questId) ?: return
        val profile = profileManager.getProfile(profileId) ?: return
        if (profile.completedQuests.contains(questId)) return

        val list = activeQuests.computeIfAbsent(profileId) { mutableListOf() }
        synchronized(list) {
            if (list.any { it.questId == questId }) return
            list.add(QuestProgress(questId, profileId))
        }
    }

    suspend fun updateQuest(
        questId: String,
        profileId: UUID,
    ) {
        val quest = getQuest(questId) ?: return
        val list = activeQuests[profileId] ?: return

        val questProgress: QuestProgress
        val shouldComplete: Boolean

        synchronized(list) {
            questProgress = list.firstOrNull { it.questId == questId } ?: return
            questProgress.currentValue++
            shouldComplete = questProgress.currentValue >= quest.targetValue
            if (shouldComplete) list.remove(questProgress)
        }

        if (!shouldComplete) return

        val profile = profileManager.getProfile(profileId) ?: return
        profileManager.addDollars(profile, quest.rewardMoney)
        profileManager.addXp(profile, quest.rewardXp.toLong())
        profileManager.addCompletedQuest(profile, questId)
    }

    private fun getQuest(questId: String): Quest? {
        val possibleQuests by QuestStore(questId)
        return possibleQuests.firstOrNull()
    }
}
