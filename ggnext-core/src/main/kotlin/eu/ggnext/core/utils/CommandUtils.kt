package eu.ggnext.core.utils

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit

fun playerArgument(name: String = "player"): RequiredArgumentBuilder<CommandSourceStack, String> =
    RequiredArgumentBuilder
        .argument<CommandSourceStack, String>(name, StringArgumentType.word())
        .suggests { _, builder ->
            val current = builder.remaining.lowercase()
            Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                if (onlinePlayer.name.lowercase().startsWith(current)) {
                    builder.suggest(onlinePlayer.name)
                }
            }
            builder.buildFuture()
        }

fun stringArgument(name: String = "string"): RequiredArgumentBuilder<CommandSourceStack, String> =
    RequiredArgumentBuilder
        .argument<CommandSourceStack, String>(name, StringArgumentType.string())

fun intArgument(name: String = "int"): RequiredArgumentBuilder<CommandSourceStack, Int> =
    RequiredArgumentBuilder
        .argument<CommandSourceStack, Int>(name, IntegerArgumentType.integer())
