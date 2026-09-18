package eu.ggnext.railway.profile

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.core.api.GGNextAPI
import eu.ggnext.railway.scoreboard.RailwayScoreBoardManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class ProfileListener(
    private val railwayProfileManager: RailwayProfileManager,
    private val profileGui: ProfileGui,
    private val scoreBoardManager: RailwayScoreBoardManager,
    private val plugin: JavaPlugin,
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
    suspend fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (profileGui.consumeNavigating(player)) {
            return
        }

        // Don't reopen GUI if player is currently entering input
        if (GGNextAPI.playerInputManager.hasActiveInput(player)) {
            return
        }

        val activeProfileId = railwayProfileManager.getActiveProfileId(player)
        if (activeProfileId == null) {
            profileGui.reopenProfileGui(player)
        } else {
            // Player has selected a profile - activate scoreboard
            plugin.launch {
                scoreBoardManager.activateForPlayer(player)
            }
        }
    }

    @EventHandler
    suspend fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        railwayProfileManager.deleteActiveProfile(player)
        profileGui.clearNavigating(player)
    }
}
