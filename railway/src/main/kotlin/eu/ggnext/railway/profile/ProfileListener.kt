package eu.ggnext.railway.profile

import eu.ggnext.core.api.GGNextAPI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ProfileListener(
    private val railwayProfileManager: RailwayProfileManager,
    private val profileGui: ProfileGui,
) : Listener {
    @EventHandler
    suspend fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        if (railwayProfileManager.getActiveProfile(player) == null) {
            profileGui.openProfileGui(player)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (profileGui.consumeNavigating(player)) {
            return
        }

        // Don't reopen GUI if player is currently entering input
        if (GGNextAPI.playerInputManager.hasActiveInput(player)) {
            return
        }

        if (railwayProfileManager.getActiveProfile(player) == null) {
            profileGui.reopenProfileGui(player)
        }
    }

    @EventHandler
    suspend fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        railwayProfileManager.deleteActiveProfile(player)
    }
}
