package de.ggnext.velocityCore.features.players.listener

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import de.ggnext.sdk.feature.PartySdk
import de.ggnext.sdk.feature.PlayersSdk
import de.ggnext.sdk.feature.PunishmentSdk
import de.ggnext.velocityCore.config.VelocityConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class PlayerListener(
    private val players: PlayersSdk,
    private val punishment: PunishmentSdk,
    private val party: PartySdk,
    private val config: VelocityConfig,
) {
    @Subscribe
    suspend fun onPostCreation(event: ServerPostConnectEvent) {
        if (event.previousServer != null) return

        val player = event.player
        players.login(player.uniqueId, player.username)
    }

    @Subscribe
    suspend fun onJoinListener(event: LoginEvent) {
        val player = event.player

        if (config.maintenance.global && !player.hasPermission("maintenance.join")) {
            event.result =
                ResultedEvent.ComponentResult.denied(
                    Component.text("Currently under maintenance.", NamedTextColor.RED),
                )
            return
        }
    }

    @Subscribe
    suspend fun onPreJoinListener(event: PreLoginEvent) {
        val uniqueId = event.uniqueId
        if (uniqueId == null) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not logged in!", NamedTextColor.RED))
            return
        }

        val activeBan =
            try {
                punishment.activeBan(uniqueId)
            } catch (_: Exception) {
                event.result =
                    PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text("Login service unavailable, please try again later.", NamedTextColor.RED),
                    )
                return
            }

        activeBan?.let {
            event.result =
                PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text("You are banned: ${it.reason}", NamedTextColor.RED),
                )
            return
        }
    }

    @Subscribe
    suspend fun onLeaveListener(event: DisconnectEvent) {
        val player = event.player
        party.leave(player.uniqueId)
        players.savePlaytime(player.uniqueId)
    }
}
