package eu.ggnext.core.input

import com.github.shynixn.mccoroutine.bukkit.launch
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listens for player chat events and routes them to PlayerInputManager
 * for players with active input requests.
 */
class PlayerInputListener(
    private val plugin: JavaPlugin,
    private val inputManager: PlayerInputManager,
) : Listener {
    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        if (!inputManager.hasActiveInput(player)) return

        // Check if this player has an active input session
        // Cancel the event so others don't see it
        event.isCancelled = true

        // Extract text from component
        val message = event.message().toString()

        // Process the input asynchronously
        plugin.launch {
            inputManager.processInput(player, message)
        }
    }
}
