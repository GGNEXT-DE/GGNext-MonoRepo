package eu.ggnext.railway.trade

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.playerArgument
import eu.ggnext.core.utils.toPlayer
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class TradeCommand(
    private val tradeManager: TradeManager,
    private val tradeGui: TradeGui,
) {
    val command =
        Commands
            .literal("trade")
            .requires { it.sender is Player }
            .then(
                Commands
                    .literal("accept")
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val request = tradeManager.getFirstValidRequestForTarget(player.uniqueId)
                        handleAccept(player, request)
                        Command.SINGLE_SUCCESS
                    }.then(
                        playerArgument("sender")
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val senderName = StringArgumentType.getString(ctx, "sender")
                                val sender = Bukkit.getPlayer(senderName)

                                if (sender == null) {
                                    val msg by TranslationStore("translations.player_not_found")
                                    player.sendMessage(msg.get(player.language()))
                                    return@executes 1
                                }
                                val request = tradeManager.getValidRequestFromSender(sender.uniqueId, player.uniqueId)
                                handleAccept(player, request)
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                Commands
                    .literal("deny")
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val request = tradeManager.getFirstValidRequestForTarget(player.uniqueId)
                        handleDeny(player, request)
                        Command.SINGLE_SUCCESS
                    }.then(
                        playerArgument("sender")
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val senderName = StringArgumentType.getString(ctx, "sender")
                                val sender = Bukkit.getPlayer(senderName)

                                if (sender == null) {
                                    val msg by TranslationStore("translations.player_not_found")
                                    player.sendMessage(msg.get(player.language()))
                                    return@executes 1
                                }
                                val request = tradeManager.getValidRequestFromSender(sender.uniqueId, player.uniqueId)
                                handleDeny(player, request)
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                playerArgument("target")
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val targetName = StringArgumentType.getString(ctx, "target")
                        val target = Bukkit.getPlayer(targetName)

                        if (target == null) {
                            val msg by TranslationStore("translations.player_not_found")
                            player.sendMessage(msg.get(player.language()))
                            return@executes 1
                        }

                        if (player == target) {
                            val msg by TranslationStore("translations.railway.trade.self_trade")
                            player.sendMessage(msg.get(player.language()))
                            return@executes 1
                        }

                        if (tradeManager.getValidRequestFromSender(player.uniqueId, target.uniqueId) != null) {
                            val msg by TranslationStore("translations.railway.trade.exist_request")
                            player.sendMessage(msg.get(player.language(), listOf(player.name)))
                            return@executes 1
                        }

                        tradeManager.createRequest(player.uniqueId, target.uniqueId)

                        val msgSent by TranslationStore("translations.railway.trade.request_sent")
                        player.sendMessage(msgSent.get(player.language(), listOf(target.name)))

                        val msgReceived by TranslationStore("translations.railway.trade.request_received")
                        target.sendMessage(msgReceived.get(target.language(), listOf(player.name)))

                        val msgActions by TranslationStore("translations.railway.trade.request_actions")
                        target.sendMessage(msgActions.get(target.language(), listOf(player.name)))
                        Command.SINGLE_SUCCESS
                    },
            ).build()

    private fun handleAccept(
        player: Player,
        request: TradeRequest?,
    ) {
        if (request == null) {
            val msg by TranslationStore("translations.railway.trade.no_request")
            player.sendMessage(msg.get(player.language()))
            return
        }

        val sender = request.sender.toPlayer() ?: return
        if (!sender.isOnline) {
            val msg by TranslationStore("translations.player_offline")
            player.sendMessage(msg.get(player.language()))
            tradeManager.removeRequest(sender.uniqueId, player.uniqueId)
            return
        }

        val session = tradeManager.startSession(sender.uniqueId, player.uniqueId)

        val msgTarget by TranslationStore("translations.railway.trade.started_target")
        player.sendMessage(msgTarget.get(player.language(), listOf(sender.name)))

        val msgSender by TranslationStore("translations.railway.trade.started_sender")
        sender.sendMessage(msgSender.get(sender.language(), listOf(player.name)))

        tradeGui.openTrade(session)
    }

    private fun handleDeny(
        player: Player,
        request: TradeRequest?,
    ) {
        if (request == null) {
            val msg by TranslationStore("translations.railway.trade.no_request")
            player.sendMessage(msg.get(player.language()))
            return
        }

        val sender = request.sender.toPlayer() ?: return
        tradeManager.removeRequest(sender.uniqueId, player.uniqueId)

        val msgTarget by TranslationStore("translations.railway.trade.denied_target")
        player.sendMessage(msgTarget.get(player.language(), listOf(sender.name)))

        if (sender.isOnline) {
            val msgSender by TranslationStore("translations.railway.trade.denied_sender")
            sender.sendMessage(msgSender.get(sender.language(), listOf(player.name)))
        }
    }
}
