package de.ggnext.core.tab

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class TabListener(
    private val tabManager: TabManager,
) : Listener {

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        tabManager.setTab(event.player)
    }
}