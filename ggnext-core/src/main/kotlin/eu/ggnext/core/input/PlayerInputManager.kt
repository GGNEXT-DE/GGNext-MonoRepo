package eu.ggnext.core.input

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.LogControl
import eu.ggnext.common.logging.warn
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.language
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias InputCallback = suspend (String) -> Unit

/**
 * Manages player text input via chat with support for GUI integration.
 * Allows players to enter text in-game, with results delivered via callback.
 * Supports automatically reopening a previous GUI after input completes.
 */
class PlayerInputManager(
    private val plugin: JavaPlugin,
) {
    private val activeInputs = ConcurrentHashMap<UUID, InputSession>()
    private val promptKey by TranslationStore("translations.core.input.prompt")
    private val inputCancelledKey by TranslationStore("translations.core.input.cancelled")
    private val inputAcceptedKey by TranslationStore("translations.core.input.accepted")
    private val logger = LogControl.logger(this::class.java)

    data class InputSession(
        val playerId: UUID,
        val callback: InputCallback,
        val previousGuiReopener: (suspend () -> Unit)?,
        val createdAt: Long = System.currentTimeMillis(),
    )

    /**
     * Request player text input via chat.
     *
     * @param player The player to request input from
     * @param callback Lambda to call with the input text when complete
     * @param previousGuiReopener Optional lambda to call to reopen the previous GUI
     */
    suspend fun requestInput(
        player: Player,
        callback: InputCallback,
        previousGuiReopener: (suspend () -> Unit)? = null,
    ) {
        if (activeInputs.containsKey(player.uniqueId)) {
            player.sendMessage(
                Component.text("You already have an active input request!", NamedTextColor.RED),
            )
            return
        }

        activeInputs[player.uniqueId] =
            InputSession(
                playerId = player.uniqueId,
                callback = callback,
                previousGuiReopener = previousGuiReopener,
            )

        player.sendMessage(
            Component
                .empty()
                .append(promptKey.get(player.language()).color(NamedTextColor.AQUA))
                .append(Component.text(" (Type 'cancel' to cancel)", NamedTextColor.GRAY)),
        )
    }

    /**
     * Process player chat input. Called by PlayerInputListener.
     */
    suspend fun processInput(
        player: Player,
        message: String,
    ) {
        val session = activeInputs[player.uniqueId] ?: return

        when (message.lowercase()) {
            "cancel" -> {
                cancelInput(player, session)
            }

            else -> {
                completeInput(player, session, message)
            }
        }
    }

    private suspend fun completeInput(
        player: Player,
        session: InputSession,
        message: String,
    ) {
        activeInputs.remove(player.uniqueId)

        try {
            session.callback(message)
            player.sendMessage(
                inputAcceptedKey.get(player.language()).color(NamedTextColor.GREEN),
            )
        } catch (e: Exception) {
            logger.warn("Error processing input for ${player.name}: ${e.message}")
            player.sendMessage(
                Component.text("An error occurred processing your input", NamedTextColor.RED),
            )
        }

        // Reopen previous GUI if provided
        session.previousGuiReopener?.let {
            plugin.launch {
                delay(50)
                try {
                    it()
                } catch (e: Exception) {
                    logger.warn("Error reopening GUI for ${player.name}: ${e.message}")
                }
            }
        }
    }

    private suspend fun cancelInput(
        player: Player,
        session: InputSession,
    ) {
        activeInputs.remove(player.uniqueId)
        player.sendMessage(
            inputCancelledKey.get(player.language()).color(NamedTextColor.YELLOW),
        )

        // Reopen previous GUI if provided
        session.previousGuiReopener?.let {
            plugin.launch {
                delay(50)
                try {
                    it()
                } catch (e: Exception) {
                    logger.warn("Error reopening GUI for ${player.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if a player has an active input request.
     */
    fun hasActiveInput(player: Player): Boolean = activeInputs.containsKey(player.uniqueId)

    /**
     * Cleanup input sessions (called on plugin disable).
     */
    fun shutdown() {
        activeInputs.clear()
    }
}
