package eu.ggnext.velocityCore.features.punishment.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import eu.ggnext.velocityCore.features.punishment.HistoryFormatter
import eu.ggnext.velocityCore.features.punishment.PunishmentHistoryFilter
import eu.ggnext.velocityCore.features.punishment.PunishmentManager
import eu.ggnext.velocityCore.utils.CommandUtils
import eu.ggnext.velocityCore.utils.asPlayerOrNull
import eu.ggnext.velocityCore.utils.filterArgument
import eu.ggnext.velocityCore.utils.playerArgument
import eu.ggnext.velocityCore.utils.siteArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HistoryCommand(
    private val commandUtils: CommandUtils,
    private val punishmentManager: PunishmentManager,
    private val proxy: ProxyServer,
    private val scope: CoroutineScope,
) {
    val history =
        BrigadierCommand
            .literalArgumentBuilder("history")
            .requires { it.hasPermission("ggnext.velocity.punishment.history") }
            .then(
                playerArgument(proxy)
                    .executes { ctx ->
                        val source = ctx.source.asPlayerOrNull() ?: return@executes 1
                        val targetName = ctx.getArgument("player", String::class.java)
                        sendHistory(source, targetName, "all")
                    }.then(
                        filterArgument()
                            .executes { ctx ->
                                val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                                val targetName = ctx.getArgument("player", String::class.java)
                                val filterInput = ctx.getArgument("filter", String::class.java)

                                sendHistory(player, targetName, filterInput)
                            }.then(
                                siteArgument()
                                    .executes { ctx ->
                                        val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                                        val targetName = ctx.getArgument("player", String::class.java)
                                        val filterInput = ctx.getArgument("filter", String::class.java)
                                        val siteInput = ctx.getArgument("site", Int::class.java)

                                        sendHistory(player, targetName, filterInput, siteInput)
                                    },
                            ),
                    ),
            ).build()

    private fun sendHistory(
        player: Player,
        targetName: String,
        filterInput: String,
        site: Int = 1,
    ): Int {
        scope.launch {
            val filter =
                runCatching { PunishmentHistoryFilter.valueOf(filterInput.uppercase()) }.getOrDefault(
                    PunishmentHistoryFilter.ALL,
                )
            val targetUUID = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch
            val history = punishmentManager.getHistory(targetUUID, filter)

            HistoryFormatter.format(player, targetName, filter, history, site).forEach(player::sendMessage)
        }

        return Command.SINGLE_SUCCESS
    }
}
