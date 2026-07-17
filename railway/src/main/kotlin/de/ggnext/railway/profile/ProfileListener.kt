package de.ggnext.railway.profile

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ProfileListener(
    private val railwayProfileManager: RailwayProfileManager,
    private val profileGui: ProfileGui,
) : Listener {
    @EventHandler
    suspend fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        val profiles = railwayProfileManager.getProfiles(player)
        if (profiles.isEmpty()) {
            railwayProfileManager.createProfile(player, profileGui.profileNames.random())
        }
        profileGui.openProfileGui(player)
    }

    @EventHandler
    suspend fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        railwayProfileManager.deleteActiveProfile(player)
    }
}
