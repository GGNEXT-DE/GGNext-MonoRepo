package eu.ggnext.core.scoreboard

import eu.ggnext.common.logging.log
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ScoreBoardListener(
    private val scoreBoardManager: ScoreBoardManager,
) : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        log.info("[GGNextCore] Creating scoreboard skeleton for ${player.name}")
        // Always create scoreboard - sub-plugins (like Railway) will handle the content
        scoreBoardManager.addPlayer(player)
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        scoreBoardManager.removePlayer(player)
    }
}
