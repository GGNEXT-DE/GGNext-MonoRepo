package eu.ggnext.core.scoreboard

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ScoreBoardListener(
    private val scoreBoardManager: ScoreBoardManager,
) : Listener {
    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        scoreBoardManager.addPlayer(player)
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        scoreBoardManager.removePlayer(player)
    }
}
