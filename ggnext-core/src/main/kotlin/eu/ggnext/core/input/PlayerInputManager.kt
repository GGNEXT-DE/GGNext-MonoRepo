package eu.ggnext.core.input

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.job.Job
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.api.GGNextAPI
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
    companion object {
        private const val MAX_INPUT_LENGTH = 256
        private const val INPUT_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private const val CLEANUP_INTERVAL_MS = 60 * 1000L // 1 minute
    }

    private val activeInputs = ConcurrentHashMap<UUID, InputSession>()
    private val promptKey by TranslationStore("translations.core.input.prompt")
    private val inputCancelledKey by TranslationStore("translations.core.input.cancelled")
    private val inputAcceptedKey by TranslationStore("translations.core.input.accepted")
    private val alreadyActiveKey by TranslationStore("translations.core.input.already_active")

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
                alreadyActiveKey.get(player.language()).color(NamedTextColor.RED),
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
        // Input validation
        if (message.isEmpty()) return
        if (message.length > MAX_INPUT_LENGTH) {
            player.sendMessage(
                Component.text("Input is too long (max $MAX_INPUT_LENGTH characters)", NamedTextColor.RED),
            )
            return
        }
        if (message.startsWith("/")) {
            player.sendMessage(
                Component.text("Commands are not allowed as input", NamedTextColor.RED),
            )
            return
        }

        when (message.lowercase()) {
            "cancel" -> {
                cancelInput(player)
            }

            else -> {
                completeInput(player, message)
            }
        }
    }

    private suspend fun completeInput(
        player: Player,
        message: String,
    ) {
        // Atomic remove to prevent race conditions
        val session = activeInputs.remove(player.uniqueId) ?: return

        try {
            session.callback(message)
            player.sendMessage(
                inputAcceptedKey.get(player.language()).color(NamedTextColor.GREEN),
            )
        } catch (e: Exception) {
            log.warn("Error processing input for ${player.name}: ${e.message}")
            player.sendMessage(
                Component.text("An error occurred processing your input", NamedTextColor.RED),
            )
        }

        // Reopen previous GUI if provided
        reopenPreviousGui(session, player)
    }

    /**
     * Cancel input for a player (called when player quits or on request).
     */
    suspend fun cancelInput(player: Player) {
        // Atomic remove to prevent race conditions
        val session = activeInputs.remove(player.uniqueId) ?: return

        if (player.isOnline) {
            player.sendMessage(
                inputCancelledKey.get(player.language()).color(NamedTextColor.YELLOW),
            )
            reopenPreviousGui(session, player)
        }
    }

    private fun reopenPreviousGui(
        session: InputSession,
        player: Player,
    ) {
        session.previousGuiReopener?.let {
            plugin.launch {
                delay(50)
                try {
                    it()
                } catch (e: Exception) {
                    log.warn("Error reopening GUI for ${player.name}: ${e.message}")
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
     * Also starts a background task to cleanup expired sessions.
     */
    fun shutdown() {
        activeInputs.clear()
    }

    /**
     * Start cleanup task to remove expired input sessions.
     */
    fun startCleanupTask() {
        GGNextAPI.jobManager.registerJob(PlayerInputCleanupJob())
    }

    /**
     * Background job that periodically removes expired input sessions.
     */
    private inner class PlayerInputCleanupJob : Job {
        override val id = "player-input-cleanup-job"
        override val interval = 60 // 1 minute in seconds
        override var lastRun: Long = System.currentTimeMillis()

        override suspend fun execute() {
            val now = System.currentTimeMillis()
            activeInputs.forEach { (uuid, session) ->
                if (now - session.createdAt > INPUT_TIMEOUT_MS) {
                    activeInputs.remove(uuid)
                    log.warn("Cleaned up expired input session for player $uuid after timeout")
                }
            }
        }
    }
}
