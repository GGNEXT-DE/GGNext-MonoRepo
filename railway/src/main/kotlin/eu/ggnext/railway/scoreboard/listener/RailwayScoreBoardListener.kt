package eu.ggnext.railway.scoreboard.listener

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
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
    companion object {
        private const val PROFILE_LOAD_ATTEMPTS = 20
        private const val PROFILE_LOAD_RETRY_DELAY_MS = 100L
        private const val PROFILE_LOAD_TIMEOUT_MS = (PROFILE_LOAD_ATTEMPTS * PROFILE_LOAD_RETRY_DELAY_MS)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        log.info("Railway: Preparing scoreboard for ${event.player.name}")
        plugin.launch {
            val player = event.player

            // Wait for active profile to be set by ProfileGui
            var attempts = 0
            while (profileManager.getActiveProfileId(player) == null && attempts < PROFILE_LOAD_ATTEMPTS) {
                attempts++
                kotlinx.coroutines.delay(PROFILE_LOAD_RETRY_DELAY_MS)
            }

            val activeProfileId =
                try {
                    profileManager.getActiveProfileId(player)
                } catch (e: Exception) {
                    log.warn("Railway: Error loading active profile for ${player.name}: ${e.message}")
                    null
                }
            if (activeProfileId != null) {
                log.info("Railway: Updating scoreboard for ${player.name}")
                scoreBoardManager.activateForPlayer(player)
            } else {
                log.warn("Railway: No active profile for ${player.name} after ${PROFILE_LOAD_TIMEOUT_MS}ms")
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        log.info("Railway: Deactivating scoreboard for ${event.player.name}")
        scoreBoardManager.deactivateForPlayer(event.player)
    }
}
