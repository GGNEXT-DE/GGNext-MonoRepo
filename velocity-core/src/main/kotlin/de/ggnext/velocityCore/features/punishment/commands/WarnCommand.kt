package de.ggnext.velocityCore.features.punishment.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.sdk.feature.PunishmentSdk
import de.ggnext.velocityCore.utils.CommandUtils
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import de.ggnext.velocityCore.utils.playerArgument
import de.ggnext.velocityCore.utils.reasonArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

class WarnCommand(
    val proxy: ProxyServer,
    val commandUtils: CommandUtils,
    val punishment: PunishmentSdk,
    private val scope: CoroutineScope,
) {
    val warn =
        BrigadierCommand
            .literalArgumentBuilder("warn")
            .requires { it.hasPermission("ggnext.velocity.punishment.warn") }
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
                                    val msg by TranslationStore("translations.punishment.warn.success")

                                    punishment.warn(targetUUID, player.uniqueId, reason)
                                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                    proxy.getPlayer(targetName).getOrNull()?.showWarning(reason)
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).build()

    private fun Player.showWarning(reason: String) {
        val title by TranslationStore("translations.punishment.warn.title")
        val msg by TranslationStore("translations.punishment.warn.chat")
        showTitle(
            Title.title(
                title.get(this.language()),
                Component.text(reason, NamedTextColor.YELLOW),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofSeconds(4),
                    Duration.ofSeconds(1),
                ),
            ),
        )
        sendMessage(msg.get(this.language(), listOf(reason)))
    }
}
