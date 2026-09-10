package eu.ggnext.railway.scoreboard.listener

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.scoreboard.RailwayScoreBoardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class RailwayScoreBoardListener(
    private val plugin: JavaPlugin,
    private val scoreBoardManager: RailwayScoreBoardManager,
    private val profileManager: RailwayProfileManager,
) : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        log.info("Railway: Preparing scoreboard for ${event.player.name}")
        plugin.launch {
            val player = event.player

            // Wait for active profile to be set by ProfileGui
            var attempts = 0
            while (profileManager.getActiveProfile(player) == null && attempts < 20) {
                attempts++
                kotlinx.coroutines.delay(100)
            }

            val activeProfile = profileManager.getActiveProfile(player)
            if (activeProfile != null) {
                log.info("Railway: Updating scoreboard for ${player.name} with profile: ${activeProfile.name}")
                scoreBoardManager.activateForPlayer(player)
            } else {
                log.info("Railway: No active profile for ${player.name}")
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        log.info("Railway: Deactivating scoreboard for ${event.player.name}")
        scoreBoardManager.deactivateForPlayer(event.player)
    }
}
