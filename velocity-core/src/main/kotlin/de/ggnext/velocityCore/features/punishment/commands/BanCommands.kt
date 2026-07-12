package de.ggnext.velocityCore.features.punishment.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.features.punishment.PunishmentManager
import de.ggnext.velocityCore.utils.CommandUtils
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.durationArgument
import de.ggnext.velocityCore.utils.language
import de.ggnext.velocityCore.utils.playerArgument
import de.ggnext.velocityCore.utils.reasonArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BanCommands(
    val commandUtils: CommandUtils,
    val punishmentManager: PunishmentManager,
    private val scope: CoroutineScope,
    private val proxy: ProxyServer,
) {
    val ban =
        BrigadierCommand
            .literalArgumentBuilder("ban")
            .requires { it.hasPermission("ggnext.velocity.punishment.ban") }
            .then(
                playerArgument(proxy)
                    .then(
                        reasonArgument()
                            .executes { ctx ->
                                val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                                val targetName = ctx.getArgument("player", String::class.java)
                                val reason = ctx.getArgument("reason", String::class.java)

                                scope.launch {
                                    val targetUUID = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

                                    if (punishmentManager.getActiveBan(targetUUID) != null) {
                                        val msg by TranslationStore("translations.punishment.ban.alreadyBanned")
                                        player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                        return@launch
                                    }

                                    punishmentManager.ban(targetUUID, player.uniqueId, reason)
                                    val msg by TranslationStore("translations.punishment.ban.disconnect")
                                    commandUtils.disconnectIfOnline(targetName, msg.get(player.language(), listOf(reason)))
                                    val msg1 by TranslationStore("translations.punishment.ban.success")
                                    player.sendMessage(msg1.get(player.language(), listOf(targetName)))
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).build()

    val tempBan =
        BrigadierCommand
            .literalArgumentBuilder("temp-ban")
            .requires { it.hasPermission("ggnext.velocity.punishment.temp-ban") }
            .then(
                playerArgument(proxy)
                    .then(
                        durationArgument()
                            .then(
                                reasonArgument()
                                    .executes { ctx ->
                                        val player = ctx.source.asPlayerOrNull() ?: return@executes 1

                                        val targetName = ctx.getArgument("player", String::class.java)
                                        val durationInput = ctx.getArgument("duration", String::class.java)
                                        val reason = ctx.getArgument("reason", String::class.java)

                                        scope.launch {
                                            val duration = commandUtils.parseDuration(durationInput, player) ?: return@launch
                                            val targetUUID = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

                                            if (punishmentManager.getActiveBan(targetUUID) != null) {
                                                val msg by TranslationStore("translations.punishment.ban.alreadyBanned")
                                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                                return@launch
                                            }

                                            punishmentManager.tempBan(targetUUID, player.uniqueId, duration, reason)
                                            val msg by TranslationStore("translations.punishment.temp-ban.disconnect")
                                            commandUtils.disconnectIfOnline(
                                                targetName,
                                                msg.get(player.language(), listOf(durationInput, reason)),
                                            )
                                            val msg1 by TranslationStore("translations.punishment.temp-ban.success")
                                            player.sendMessage(msg1.get(player.language(), listOf(targetName, durationInput)))
                                        }
                                        Command.SINGLE_SUCCESS
                                    },
                            ),
                    ),
            ).build()

    val unBan =
        BrigadierCommand
            .literalArgumentBuilder("unban")
            .requires { it.hasPermission("ggnext.velocity.punishment.un-ban") }
            .then(
                playerArgument(proxy)
                    .executes { ctx ->
                        val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                        val targetName = ctx.getArgument("player", String::class.java)

                        scope.launch {
                            val targetUUID = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

                            if (punishmentManager.revokeBan(targetUUID, player.uniqueId)) {
                                val msg by TranslationStore("translations.punishment.unban.success")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            } else {
                                val msg by TranslationStore("translations.punishment.unban.failed")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            }
                        }
                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
