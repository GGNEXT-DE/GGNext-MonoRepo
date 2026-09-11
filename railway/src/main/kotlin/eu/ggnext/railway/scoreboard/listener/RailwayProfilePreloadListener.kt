package eu.ggnext.railway.scoreboard.listener

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.profile.railwayProfileNames
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Preloads Railway profiles BEFORE GGNextCore creates the scoreboard.
 * This ensures the profile is available when RailwayScoreBoardManager tries to use it.
 */
class RailwayProfilePreloadListener(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        log.info("Railway preload: Creating/loading profile for ${event.player.name}")
        plugin.launch {
            val player = event.player
            val profiles = profileManager.getProfiles(player)
            if (profiles.isEmpty()) {
                log.info("Railway preload: Creating new profile for ${event.player.name}")
                profileManager.createProfile(player, railwayProfileNames.random())
            }
            // Set first profile as active if none is active
            if (profileManager.getActiveProfile(player) == null) {
                val activeProfile = profileManager.getProfiles(player).firstOrNull()
                if (activeProfile != null) {
                    profileManager.setActiveProfile(player, activeProfile)
                    log.info("Railway preload: Set active profile for ${player.name}: ${activeProfile.name}")
                }
            }
        }
    }
}
