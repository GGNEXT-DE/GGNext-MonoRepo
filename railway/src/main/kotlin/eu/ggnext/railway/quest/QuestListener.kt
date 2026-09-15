package eu.ggnext.railway.quest

import eu.ggnext.contentsystem.value.types.QuestTrackingType
import eu.ggnext.railway.profile.RailwayProfileManager
import io.papermc.paper.event.inventory.ItemCraftedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class QuestListener(
    private val questManager: QuestManager,
    private val profileManager: RailwayProfileManager,
) : Listener {
    private val lastCheck = ConcurrentHashMap<UUID, Long>()
    private val throttleMillis = 1000L

    @EventHandler
    suspend fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val activeProfileId = profileManager.getActiveProfileId(player) ?: return

        val quests = questManager.getActiveQuests(activeProfileId)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.ITEM_MINED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfileId)
        }
    }

    @EventHandler
    suspend fun onItemCrafted(event: ItemCraftedEvent) {
        val player = event.player
        val activeProfileId = profileManager.getActiveProfileId(player) ?: return

        val quests = questManager.getActiveQuests(activeProfileId)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.ITEM_CRAFTED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfileId)
        }
    }

    @EventHandler
    suspend fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

        val player = event.player
        val now = System.currentTimeMillis()
        val last = lastCheck[player.uniqueId] ?: 0L

        if (now - last < throttleMillis) return
        lastCheck[player.uniqueId] = now

        val activeProfileId = profileManager.getActiveProfileId(player) ?: return

        val quests = questManager.getActiveQuests(activeProfileId)
        if (quests.isEmpty()) return

        quests.filter { it.type == QuestTrackingType.BLOCKS_WALKED }.forEach { quest ->
            questManager.updateQuest(quest.questId, activeProfileId)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        lastCheck.remove(event.player.uniqueId)
    }
}
