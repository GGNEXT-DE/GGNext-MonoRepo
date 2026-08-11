package de.ggnext.velocityCore.features.maintenance.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.velocityCore.config.ConfigManager
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
                                val server = ctx.getArgument("server", String::class.java)
                                configManager.config.maintenance.servers += server
                                configManager.save()
                                ctx.source.sendMessage(Component.text("$server zu maintenance hinzugefügt!"))
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
                                val server = ctx.getArgument("server", String::class.java)
                                configManager.config.maintenance.servers -= server
                                configManager.save()
                                ctx.source.sendMessage(Component.text("$server von maintenance entfernt!"))
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("list")
                    .executes { ctx ->
                        ctx.source.sendMessage(
                            Component.text("Maintenance servers: ${configManager.config.maintenance.servers.joinToString(", ")}"),
                        )
                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
