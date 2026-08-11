package de.ggnext.velocityCore.features.hub

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.utils.language
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class HubCommand(
    private val server: ProxyServer,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("hub")
            .requires { it is Player }
            .executes { ctx ->
                val player = ctx.source as Player

                val lobbyServer = server.getServer("lobby").orElse(null)

                if (lobbyServer == null) {
                    val msg by TranslationStore("translations.velocity.hub.not_found")
                    player.sendMessage(msg.get(player.language()))
                    return@executes 0
                }

                player.createConnectionRequest(lobbyServer).connect()

                Command.SINGLE_SUCCESS
            }.build()
}
