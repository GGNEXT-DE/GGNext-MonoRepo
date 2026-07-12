package de.ggnext.velocityCore.features.players.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import de.ggnext.velocityCore.config.VelocityConfig
import de.ggnext.velocityCore.features.players.PlayerManager
import de.ggnext.velocityCore.features.punishment.PunishmentManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class PlayerListener(
    private val playerManager: PlayerManager,
    private val punishmentManager: PunishmentManager,
    private val config: VelocityConfig,
) {
    @Subscribe
    suspend fun onJoinListener(event: LoginEvent) {
        val player = event.player
        val activeBan = punishmentManager.getActiveBan(player.uniqueId)

        if (config.globalMaintenanceMode && !player.hasPermission("maintenance.join")) {
            return player.disconnect(Component.text("Currently under maintenance.", NamedTextColor.RED))
        }

        if (activeBan != null) {
            if ((activeBan.revokedBy == null && activeBan.expiresAt > System.currentTimeMillis()) ||
                (activeBan.revokedBy == null && activeBan.expiresAt == -1L)
            ) {
                return player.disconnect(Component.text("You are banned: ${activeBan.reason}", NamedTextColor.RED))
            }
        }

        if (playerManager.getPlayer(player.uniqueId) == null) {
            playerManager.createPlayer(event.player.uniqueId, player.username)
        } else {
            playerManager.loginPlayer(player.uniqueId, player.username)
        }
    }

    @Subscribe
    suspend fun onLeaveListener(event: DisconnectEvent) {
        val player = event.player
        playerManager.savePlaytime(player.uniqueId)
    }
}
