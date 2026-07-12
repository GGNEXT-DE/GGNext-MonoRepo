package de.ggnext.railway.profile

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ProfileListener(
    private val railwayProfileManager: RailwayProfileManager,
) : Listener {
    @EventHandler
    suspend fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        val profile = railwayProfileManager.getProfiles(player).firstOrNull() ?: return
        railwayProfileManager.setActiveProfile(player, profile)
    }

    @EventHandler
    suspend fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        railwayProfileManager.deleteActiveProfile(player)
    }
}
