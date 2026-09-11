package eu.ggnext.core.input

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listens for player chat events and routes them to PlayerInputManager
 * for players with active input requests.
 */
class PlayerInputListener(
    private val plugin: JavaPlugin,
    private val inputManager: PlayerInputManager,
) : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        if (!inputManager.hasActiveInput(player)) return

        try {
            // Cancel the event so others don't see it
            event.isCancelled = true

            // Extract plain text from component
            val message = PlainTextComponentSerializer.plainText().serialize(event.message())

            // Process the input asynchronously
            plugin.launch {
                inputManager.processInput(player, message)
            }
        } catch (e: Exception) {
            log.warn("Error processing player input for ${player.name}: ${e.message}")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.launch {
            inputManager.cancelInput(event.player)
        }
    }
}
