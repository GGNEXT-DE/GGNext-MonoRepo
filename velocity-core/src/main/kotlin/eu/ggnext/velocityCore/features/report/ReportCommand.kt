package eu.ggnext.velocityCore.features.report

import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import eu.ggnext.contentsystem.value.store.StringStore
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.velocityCore.utils.CommandUtils
import eu.ggnext.velocityCore.utils.asPlayerOrNull
import eu.ggnext.velocityCore.utils.language
import eu.ggnext.velocityCore.utils.playerArgument
import eu.ggnext.velocityCore.utils.reasonArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class ReportCommand(
    private val proxy: ProxyServer,
    private val scope: CoroutineScope,
    private val commandUtils: CommandUtils,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("report")
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
                                    sendWebHook(player, targetName, targetUUID, reason)

                                    notifyModerators(player, targetName, reason)

                                    val msg by TranslationStore("translations.velocity.report.reported")
                                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).build()

    private fun sendWebHook(
        player: Player,
        targetName: String,
        targetUUID: UUID,
        reason: String,
    ) {
        val webHookLink by StringStore("strings.velocity.report.webhook")

        val client =
            WebhookClientBuilder(webHookLink)
                .build()

        client.send(
            WebhookEmbedBuilder()
                .setTitle(WebhookEmbed.EmbedTitle("New Report", null))
                .setColor(0xFF0000)
                .addField(
                    WebhookEmbed.EmbedField(
                        true,
                        "Reporter",
                        "${player.username} | `${player.uniqueId}`",
                    ),
                ).addField(
                    WebhookEmbed.EmbedField(
                        true,
                        "Reported player",
                        "$targetName | `$targetUUID`",
                    ),
                ).addField(
                    WebhookEmbed.EmbedField(
                        true,
                        "Report reason",
                        reason,
                    ),
                ).build(),
        )
    }

    private fun notifyModerators(
        player: Player,
        targetName: String,
        reason: String,
    ) {
        val msg by TranslationStore("translations.velocity.report.notify")
        proxy.allPlayers.filter { it.hasPermission("group.moderator") }.forEach {
            it.sendMessage(msg.get(player.language(), listOf(player.username, targetName, reason)))
        }
    }
}
