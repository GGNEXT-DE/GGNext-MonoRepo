package eu.ggnext.velocityCore.features.punishment

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class PunishmentChatListener(
    private val punishmentManager: PunishmentManager,
) {
    @Subscribe
    suspend fun onChat(event: PlayerChatEvent) {
        val mute = punishmentManager.getActiveMute(event.player.uniqueId) ?: return

        event.result = PlayerChatEvent.ChatResult.denied()
        event.player.sendMessage(
            Component.text("You are muted: ${mute.reason}", NamedTextColor.RED),
        )
    }
}
