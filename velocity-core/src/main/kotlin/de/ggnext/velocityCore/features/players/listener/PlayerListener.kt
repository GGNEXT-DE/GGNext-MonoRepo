package de.ggnext.velocityCore.features.players.listener

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import de.ggnext.velocityCore.config.VelocityConfig
import de.ggnext.velocityCore.features.party.PartySystemManager
import de.ggnext.velocityCore.features.players.PlayerManager
import de.ggnext.velocityCore.features.punishment.PunishmentManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class PlayerListener(
    private val playerManager: PlayerManager,
    private val punishmentManager: PunishmentManager,
    private val partySystemManager: PartySystemManager,
    private val config: VelocityConfig,
) {
    @Subscribe
    suspend fun onPostCreation(event: ServerPostConnectEvent) {
        if (event.previousServer != null) return

        val player = event.player

        playerManager.getPlayer(player.uniqueId) ?: run {
            playerManager.createPlayer(player.uniqueId, player.username)
            return
        }
        playerManager.loginPlayer(player.uniqueId, player.username)
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

        punishmentManager
            .getActiveBan(uniqueId)
            ?.let { activeBan ->
                event.result =
                    PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text("You are banned: ${activeBan.reason}", NamedTextColor.RED),
                    )
                return
            }
    }

    @Subscribe
    suspend fun onLeaveListener(event: DisconnectEvent) {
        val player = event.player
        partySystemManager.leaveParty(player.uniqueId)
        playerManager.savePlaytime(player.uniqueId)
    }
}
