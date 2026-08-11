package de.ggnext.velocityCore.features.maintenance.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.config.ConfigManager
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import net.kyori.adventure.text.Component

class EditMtCommand(
    private val configManager: ConfigManager,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("maintenance")
            .requires { it.hasPermission("ggnext.velocity.maintenance") }
            .then(
                BrigadierCommand
                    .literalArgumentBuilder("add")
                    .then(
                        BrigadierCommand
                            .requiredArgumentBuilder("server", StringArgumentType.word())
                            .executes { ctx ->
                                val player = ctx.source.asPlayerOrNull() ?: return@executes 0
                                val server = ctx.getArgument("server", String::class.java)
                                configManager.config.maintenance.servers += server
                                configManager.save()

                                val msg by TranslationStore("translations.velocity.maintenance.server_added")
                                player.sendMessage(msg.get(player.language(), listOf(server)))
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("remove")
                    .then(
                        BrigadierCommand
                            .requiredArgumentBuilder("server", StringArgumentType.word())
                            .executes { ctx ->
                                val player = ctx.source.asPlayerOrNull() ?: return@executes 0
                                val server = ctx.getArgument("server", String::class.java)
                                configManager.config.maintenance.servers -= server
                                configManager.save()

                                val msg by TranslationStore("translations.velocity.maintenance.server_removed")
                                player.sendMessage(msg.get(player.language(), listOf(server)))
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("list")
                    .executes { ctx ->
                        val player = ctx.source.asPlayerOrNull() ?: return@executes 0

                        val msg by TranslationStore("translations.velocity.maintenance.server_list")
                        val servers =
                            configManager.config.maintenance.servers
                                .joinToString(", ")
                        player.sendMessage(msg.get(player.language(), listOf(servers)))
                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
