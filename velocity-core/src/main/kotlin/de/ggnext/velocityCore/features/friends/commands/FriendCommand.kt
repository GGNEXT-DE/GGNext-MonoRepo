package de.ggnext.velocityCore.features.friends.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.features.friends.results.AcceptResult
import de.ggnext.velocityCore.features.friends.results.DenyResult
import de.ggnext.velocityCore.features.friends.results.FriendRequestResult
import de.ggnext.velocityCore.features.friends.FriendSystemManager
import de.ggnext.velocityCore.features.friends.results.RemoveResult
import de.ggnext.velocityCore.utils.CommandUtils
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import de.ggnext.velocityCore.utils.playerArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.optionals.getOrNull

class FriendCommand(
    private val friendSystemManager: FriendSystemManager,
    private val commandUtils: CommandUtils,
    private val proxy: ProxyServer,
    private val scope: CoroutineScope,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("friends")
            .executes { ctx ->
                runList(ctx.source)
            }.then(
                BrigadierCommand
                    .literalArgumentBuilder("list")
                    .executes { ctx -> runList(ctx.source) },
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("add")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runAdd(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("remove")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runRemove(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("accept")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runAccept(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("deny")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runDeny(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).build()

    private fun runList(source: CommandSource): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val friends = friendSystemManager.getFriends(player.uniqueId)
            if (friends.isEmpty()) {
                val empty by TranslationStore("translations.velocity.friendsystem.list.empty")
                player.sendMessage(empty.get(player.language()))
                return@launch
            }

            val header by TranslationStore("translations.velocity.friendsystem.list.header")
            player.sendMessage(header.get(player.language(), listOf(friends.size.toString())))
            friends.forEach { friend ->
                val entry by TranslationStore("translations.velocity.friendsystem.list.entry")
                player.sendMessage(entry.get(player.language(), listOf(friend.username)))
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runAdd(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val targetId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (friendSystemManager.createRequest(player.uniqueId, targetId)) {
                FriendRequestResult.SENT -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.sent")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))

                    proxy.getPlayer(targetId).getOrNull()?.let { target ->
                        val received by TranslationStore("translations.velocity.friendsystem.add.received")
                        target.sendMessage(received.get(target.language(), listOf(player.username)))
                    }
                }

                FriendRequestResult.ACCEPTED_MUTUAL -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.mutual")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))

                    proxy.getPlayer(targetId).getOrNull()?.let { target ->
                        val other by TranslationStore("translations.velocity.friendsystem.add.mutual")
                        target.sendMessage(other.get(target.language(), listOf(player.username)))
                    }
                }

                FriendRequestResult.ALREADY_REQUESTED -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.already_requested")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                FriendRequestResult.ALREADY_FRIENDS -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.already_friends")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                FriendRequestResult.REQUESTS_DISABLED -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.requests_disabled")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                FriendRequestResult.SELF -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.add.self")
                    player.sendMessage(msg.get(player.language()))
                }

                FriendRequestResult.TARGET_NOT_FOUND -> {
                    val msg by TranslationStore("translations.player_not_found")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runRemove(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val targetId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (friendSystemManager.removeFriend(player.uniqueId, targetId)) {
                RemoveResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.remove.success")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                RemoveResult.NOT_FRIENDS -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.remove.not_friends")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runAccept(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val fromId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (friendSystemManager.acceptRequest(player.uniqueId, fromId)) {
                AcceptResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.accept.success")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))

                    proxy.getPlayer(fromId).getOrNull()?.let { from ->
                        val notify by TranslationStore("translations.velocity.friendsystem.accept.notify")
                        from.sendMessage(notify.get(from.language(), listOf(player.username)))
                    }
                }

                AcceptResult.NO_REQUEST -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.accept.no_request")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                AcceptResult.EXPIRED -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.accept.expired")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runDeny(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val fromId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (friendSystemManager.denyRequest(player.uniqueId, fromId)) {
                DenyResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.deny.success")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                DenyResult.NO_REQUEST -> {
                    val msg by TranslationStore("translations.velocity.friendsystem.deny.no_request")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }
}
