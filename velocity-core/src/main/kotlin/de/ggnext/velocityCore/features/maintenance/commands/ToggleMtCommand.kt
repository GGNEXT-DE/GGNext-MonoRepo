package de.ggnext.velocityCore.features.maintenance.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.velocityCore.config.ConfigManager
import net.kyori.adventure.text.Component

class ToggleMtCommand(
    val configManager: ConfigManager,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("toggle-maintenance")
            .requires { it.hasPermission("ggnext.velocity.maintenance") }
            .executes { ctx ->
                val sender = ctx.source

                val config = configManager.config

                config.globalMaintenanceMode = !config.globalMaintenanceMode
                configManager.save()

                sender.sendMessage(Component.text("Maintenance: ${config.globalMaintenanceMode}"))

                Command.SINGLE_SUCCESS
            }.build()
}
