package de.ggnext.railway.zone.listener

import de.ggnext.railway.zone.ZoneManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class ZoneListener(
    private val zoneManager: ZoneManager,
) : Listener {
    @EventHandler
    suspend fun onPlayerLeave(event: PlayerQuitEvent) {
        zoneManager.stopRun(event.player)
    }
}
