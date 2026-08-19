package de.ggnext.velocityCore.features.punishment

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import de.ggnext.sdk.feature.PunishmentSdk
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class PunishmentChatListener(
    private val punishment: PunishmentSdk,
) {
    @Subscribe
    suspend fun onChat(event: PlayerChatEvent) {
        val mute = punishment.activeMute(event.player.uniqueId) ?: return

        event.result = PlayerChatEvent.ChatResult.denied()
        event.player.sendMessage(
            Component.text("You are muted: ${mute.reason}", NamedTextColor.RED),
        )
    }
}
