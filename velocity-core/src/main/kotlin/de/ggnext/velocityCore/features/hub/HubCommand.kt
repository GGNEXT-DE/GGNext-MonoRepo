package de.ggnext.velocityCore.features.hub

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
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
                    player.sendMessage(Component.text("Lobby server could not be found!", NamedTextColor.RED))
                    return@executes 0
                }

                player.createConnectionRequest(lobbyServer).connect()

                Command.SINGLE_SUCCESS
            }.build()
}
