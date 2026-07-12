package de.ggnext.velocityCore.features.maintenance.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import de.ggnext.velocityCore.config.VelocityConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage

// Note: global maintenance permission check (maintenance.join) is handled in PlayerListener#onJoinListener,
// since LoginEvent is required for permission checks on the authenticating player.
class MaintenanceListener(
    val config: VelocityConfig,
) {
    @Subscribe
    fun onChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        val player = event.player
        val server =
            event.initialServer
                .orElse(null)
                ?.serverInfo
                ?.name

        if (config.maintenanceServers.contains(server) && !player.hasPermission("maintenance.join")) {
            player.disconnect(Component.text("Currently under maintenance.", NamedTextColor.RED))
        }
    }

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        if (config.globalMaintenanceMode) {
            val builder = event.ping.asBuilder()
            builder.description(
                MiniMessage.miniMessage().deserialize(
                    "<bold>  </bold>            <green><bold>GGNEXT NETWORK</bold></green><gray>" +
                        " [</gray><red>1.21.11+</red><gray>]</gray>\n" +
                        "                      <red>Wartungsarbeiten</red>",
                ),
            )
            builder.version(ServerPing.Version(-1, "maintenance"))
            builder.maximumPlayers(0)
            builder.onlinePlayers(0)
            event.ping = builder.build()
        }
    }
}
