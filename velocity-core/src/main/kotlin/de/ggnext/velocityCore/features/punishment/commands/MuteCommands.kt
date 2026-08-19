package de.ggnext.velocityCore.features.punishment.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.sdk.feature.PunishmentSdk
import de.ggnext.velocityCore.utils.CommandUtils
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.durationArgument
import de.ggnext.velocityCore.utils.language
import de.ggnext.velocityCore.utils.playerArgument
import de.ggnext.velocityCore.utils.reasonArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import kotlin.jvm.optionals.getOrNull

class MuteCommands(
    val proxy: ProxyServer,
    val commandUtils: CommandUtils,
    val punishment: PunishmentSdk,
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

                                    if (punishment.activeMute(targetUUID) != null) {
                                        val msg by TranslationStore("translations.punishment.mute.alreadyMuted")
                                        player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                        return@launch
                                    }

                                    punishment.mute(targetUUID, player.uniqueId, reason)
                                    val msg by TranslationStore("translations.punishment.mute.success")
                                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                    val msg1 by TranslationStore("translations.punishment.mute.muted")
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

                                            if (punishment.activeMute(targetUUID) != null) {
                                                val msg by TranslationStore("translations.punishment.mute.alreadyMuted")
                                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                                return@launch
                                            }

                                            punishment.tempMute(targetUUID, player.uniqueId, duration, reason)
                                            val msg by TranslationStore("translations.punishment.temp-mute.success")
                                            player.sendMessage(msg.get(player.language(), listOf(targetName, durationInput)))
                                            val msg1 by TranslationStore("translations.punishment.temp-mute.muted")
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

                            if (punishment.unmute(targetUUID, player.uniqueId)) {
                                val msg by TranslationStore("translations.punishment.unmute.success")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            } else {
                                val msg by TranslationStore("translations.punishment.unmute.failed")
                                player.sendMessage(msg.get(player.language(), listOf(targetName)))
                            }
                        }
                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
