package eu.ggnext.railway.quest

import eu.ggnext.contentsystem.value.types.QuestTrackingType
import eu.ggnext.railway.profile.RailwayProfileManager
import io.papermc.paper.event.inventory.ItemCraftedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerMoveEvent

class QuestListener(
    private val questManager: QuestManager,
    private val profileManager: RailwayProfileManager,
) : Listener {
    @EventHandler
    suspend fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val activeProfile = profileManager.getActiveProfile(player) ?: return

        val quests = questManager.getActiveQuests(activeProfile.id)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.ITEM_MINED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfile.id)
        }
    }

    @EventHandler
    suspend fun onItemCrafted(event: ItemCraftedEvent) {
        val player = event.player
        val activeProfile = profileManager.getActiveProfile(player) ?: return

        val quests = questManager.getActiveQuests(activeProfile.id)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.ITEM_CRAFTED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfile.id)
        }
    }

    @EventHandler
    suspend fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

        val player = event.player
        val activeProfile = profileManager.getActiveProfile(player) ?: return

        val quests = questManager.getActiveQuests(activeProfile.id)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.BLOCKS_WALKED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfile.id)
        }
    }
}
