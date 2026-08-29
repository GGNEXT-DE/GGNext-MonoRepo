package eu.ggnext.velocityCore.features.maintenance.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.velocityCore.config.ConfigManager
import eu.ggnext.velocityCore.utils.asPlayerOrNull
import eu.ggnext.velocityCore.utils.language

class ToggleMtCommand(
    val configManager: ConfigManager,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("toggle-maintenance")
            .requires { it.hasPermission("ggnext.velocity.maintenance") }
            .executes { ctx ->
                val player = ctx.source.asPlayerOrNull() ?: return@executes 0

                val config = configManager.config

                config.maintenance.global = !config.maintenance.global
                configManager.save()

                val msg by TranslationStore("translations.velocity.maintenance.global")
                player.sendMessage(msg.get(player.language(), listOf(config.maintenance.global.toString())))

                Command.SINGLE_SUCCESS
            }.build()
}
