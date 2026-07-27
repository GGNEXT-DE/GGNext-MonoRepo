package de.ggnext.railway.quest

import de.ggnext.contentsystem.value.store.QuestStore
import java.util.UUID

class QuestManager {
    private val quests by QuestStore("quest")
    private val activeQuests = mutableMapOf<UUID, MutableList<QuestProgress>>()

    fun getActiveQuests(profileId: UUID): List<QuestProgress> = activeQuests[profileId] ?: emptyList()

    fun startQuest(questId: String, profileId: UUID) {
        quests.forEach { quest ->
            
        }
        activeQuests.computeIfAbsent(profileId) { mutableListOf() }.add(QuestProgress(questId = questId, profileId =  profileId))
    }
}