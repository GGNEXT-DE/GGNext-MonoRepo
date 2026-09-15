package eu.ggnext.railway.profile

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
    // Profile creation on join is handled exclusively by RailwayProfilePreloadListener
    // (HIGHEST priority) to avoid a create-then-check race that could produce duplicate profiles.
    @EventHandler
    suspend fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        if (railwayProfileManager.getActiveProfileId(player) == null) {
            profileGui.openProfileGui(player)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (profileGui.consumeNavigating(player)) {
            return
        }

        if (railwayProfileManager.getActiveProfileId(player) == null) {
            profileGui.reopenProfileGui(player)
        }
    }

    @EventHandler
    suspend fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        railwayProfileManager.deleteActiveProfile(player)
        profileGui.clearNavigating(player)
    }
}
