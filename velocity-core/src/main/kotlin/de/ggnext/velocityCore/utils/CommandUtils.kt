package de.ggnext.velocityCore.utils

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.features.players.PlayerManager
import de.ggnext.velocityCore.features.punishment.DurationParser
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

fun CommandSource.asPlayerOrNull(): Player? = this as? Player

class CommandUtils(
    private val proxy: ProxyServer,
    private val playerManager: PlayerManager,
) {
    suspend fun resolveTargetUUID(
        username: String,
        player: Player,
    ): UUID? {
        val onlinePlayer = proxy.getPlayer(username).getOrNull()
        if (onlinePlayer != null) {
            return onlinePlayer.uniqueId
        }

        val storedPlayer = playerManager.getPlayer(username)
        if (storedPlayer != null) {
            return storedPlayer.id
        }

        val msg by TranslationStore("translations.player_not_found")
        player.sendMessage(msg.get(player.language(), listOf(username)))
        return null
    }

    fun disconnectIfOnline(
        username: String,
        message: Component,
    ) {
        proxy.getPlayer(username).getOrNull()?.disconnect(message)
    }

    fun parseDuration(
        input: String,
        player: Player,
    ): Long? =
        try {
            DurationParser.parse(input)
        } catch (_: IllegalArgumentException) {
            player.sendMessage(Component.text("Invalid duration '$input'. Use values like 30m, 12h, 7d or 2w."))
            null
        }
}

fun playerArgument(proxy: ProxyServer) =
    BrigadierCommand
        .requiredArgumentBuilder("player", StringArgumentType.word())
        .suggests { _, builder ->
            proxy.allPlayers.forEach { builder.suggest(it.username) }
            builder.buildFuture()
        }

fun reasonArgument() = BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.greedyString())

fun durationArgument() =
    BrigadierCommand
        .requiredArgumentBuilder("duration", StringArgumentType.word())
        .suggests { _, builder ->
            listOf("30m", "1h", "12h", "1d", "7d", "30d").forEach(builder::suggest)
            builder.buildFuture()
        }

fun filterArgument() =
    BrigadierCommand
        .requiredArgumentBuilder("filter", StringArgumentType.word())
        .suggests { _, builder ->
            listOf("active", "inactive", "all").forEach(builder::suggest)
            builder.buildFuture()
        }

fun siteArgument() =
    BrigadierCommand
        .requiredArgumentBuilder("site", IntegerArgumentType.integer())
