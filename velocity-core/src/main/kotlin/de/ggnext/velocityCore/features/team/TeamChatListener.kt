package de.ggnext.velocityCore.features.team

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import kotlin.jvm.optionals.getOrNull

class TeamChatListener(
    private val server: ProxyServer,
) {
    private val teamRegex = Regex("""@team\b|@t\b""")

    @Subscribe
    fun onMessage(event: PlayerChatEvent) {
        val playerMessage = event.message
        val player = event.player

        if (!teamRegex.containsMatchIn(playerMessage)) return
        if (!player.hasPermission("group.team")) return

        event.setResult(PlayerChatEvent.ChatResult.denied())

        val cleanMessage = playerMessage.replace(teamRegex, "").trim()

        val serverName =
            event.player.currentServer
                .getOrNull()
                ?.serverInfo
                ?.name ?: "Unknown Server"
        val hoverText = Component.text(serverName.uppercase(), NamedTextColor.WHITE)
        val message =
            MiniMessage
                .miniMessage()
                .deserialize("<gold>[Team] <green>${player.username}: <gray>$cleanMessage")
                .hoverEvent(hoverText)

        server.allPlayers
            .filter { it.hasPermission("group.team") }
            .forEach { it.sendMessage(message) }
    }
}
