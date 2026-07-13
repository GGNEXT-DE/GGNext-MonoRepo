package de.ggnext.railway.trade

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.Suggestions
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.language
import de.ggnext.core.utils.player
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

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
                        Commands
                            .argument("sender", StringArgumentType.word())
                            .suggests { _, builder -> provideOnlinePlayerSuggestions(builder) }
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val senderName = StringArgumentType.getString(ctx, "sender")
                                val sender = Bukkit.getPlayer(senderName)

                                if (sender == null) {
                                    val msg by TranslationStore("translations.railway.trade.player_not_found")
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
                        Commands
                            .argument("sender", StringArgumentType.word())
                            .suggests { _, builder -> provideOnlinePlayerSuggestions(builder) }
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val senderName = StringArgumentType.getString(ctx, "sender")
                                val sender = Bukkit.getPlayer(senderName)

                                if (sender == null) {
                                    val msg by TranslationStore("translations.railway.trade.player_not_found")
                                    player.sendMessage(msg.get(player.language()))
                                    return@executes 1
                                }
                                val request = tradeManager.getValidRequestFromSender(sender.uniqueId, player.uniqueId)
                                handleDeny(player, request)
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                Commands
                    .argument("target", StringArgumentType.word())
                    .suggests { _, builder -> provideOnlinePlayerSuggestions(builder) }
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val targetName = StringArgumentType.getString(ctx, "target")
                        val target = Bukkit.getPlayer(targetName)

                        if (target == null) {
                            val msg by TranslationStore("translations.railway.trade.player_not_found")
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

        val sender = request.sender.player() ?: return
        if (!sender.isOnline) {
            val msg by TranslationStore("translations.railway.trade.sender_offline")
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

        val sender = request.sender.player() ?: return
        tradeManager.removeRequest(sender.uniqueId, player.uniqueId)

        val msgTarget by TranslationStore("translations.railway.trade.denied_target")
        player.sendMessage(msgTarget.get(player.language(), listOf(sender.name)))

        if (sender.isOnline) {
            val msgSender by TranslationStore("translations.railway.trade.denied_sender")
            sender.sendMessage(msgSender.get(sender.language(), listOf(player.name)))
        }
    }

    private fun provideOnlinePlayerSuggestions(
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val current = builder.remaining.lowercase()
        Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
            if (onlinePlayer.name.lowercase().startsWith(current)) {
                builder.suggest(onlinePlayer.name)
            }
        }
        val future = CompletableFuture<Suggestions>()
        future.complete(builder.build())
        return future
    }
}
