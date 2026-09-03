package eu.ggnext.velocityCore.features.punishment.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.ProxyServer
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.velocityCore.features.punishment.PunishmentManager
import eu.ggnext.velocityCore.utils.CommandUtils
import eu.ggnext.velocityCore.utils.asPlayerOrNull
import eu.ggnext.velocityCore.utils.durationArgument
import eu.ggnext.velocityCore.utils.language
import eu.ggnext.velocityCore.utils.playerArgument
import eu.ggnext.velocityCore.utils.reasonArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.optionals.getOrNull

class MuteCommands(
    private val proxy: ProxyServer,
    private val commandUtils: CommandUtils,
    private val punishmentManager: PunishmentManager,
    private val scope: CoroutineScope,
) {
    val mute =
        BrigadierCommand
            .literalArgumentBuilder("mute")
            .requires { it.hasPermission("ggnext.velocity.punishment.mute") }
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

                                    if (punishmentManager.getActiveMute(targetUUID) != null) {
                                        val msg by TranslationStore("translations.velocity.punishment.mute.already_muted")
                                        player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                        return@launch
                                    }

                                    punishmentManager.mute(targetUUID, player.uniqueId, reason)
                                    val msg by TranslationStore("translations.velocity.punishment.mute.success")
                                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                    val msg1 by TranslationStore("translations.velocity.punishment.mute.muted")
                                    proxy
                                        .getPlayer(targetName)
                                        .getOrNull()
                                        ?.sendMessage(msg1.get(player.language(), listOf(reason)))
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).build()

    val tempMute =
        BrigadierCommand
            .literalArgumentBuilder("temp-mute")
            .requires { it.hasPermission("ggnext.punishment.temp-mute") }
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

                                            if (punishmentManager.getActiveMute(targetUUID) != null) {
                                                val msg by TranslationStore("translations.velocity.punishment.mute.already_muted")
                                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                                return@launch
                                            }

                                            punishmentManager.tempMute(targetUUID, player.uniqueId, duration, reason)
                                            val msg by TranslationStore("translations.velocity.punishment.temp_mute.success")
                                            player.sendMessage(msg.get(player.language(), listOf(targetName, durationInput)))
                                            val msg1 by TranslationStore("translations.velocity.punishment.temp_mute.muted")
                                            proxy
                                                .getPlayer(targetName)
                                                .getOrNull()
                                                ?.sendMessage(msg1.get(player.language(), listOf(durationInput, reason)))
                                        }
                                        Command.SINGLE_SUCCESS
                                    },
                            ),
                    ),
            ).build()

    val unMute =
        BrigadierCommand
            .literalArgumentBuilder("unmute")
            .requires { it.hasPermission("ggnext.punishment.unmute") }
            .then(
                playerArgument(proxy)
                    .executes { ctx ->
                        val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                        val targetName = ctx.getArgument("player", String::class.java)

                        scope.launch {
                            val targetUUID = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

                            if (punishmentManager.revokeMute(targetUUID, player.uniqueId)) {
                                val msg by TranslationStore("translations.velocity.punishment.unmute.success")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            } else {
                                val msg by TranslationStore("translations.velocity.punishment.unmute.failed")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            }
                        }
                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
