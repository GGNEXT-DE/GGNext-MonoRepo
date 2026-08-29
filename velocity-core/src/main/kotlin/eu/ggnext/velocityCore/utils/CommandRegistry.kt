package eu.ggnext.velocityCore.utils

import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.command.CommandSource
import eu.ggnext.velocityCore.VelocityCore

class CommandRegistry(
    private val commandManager: CommandManager,
    private val velocityCore: VelocityCore,
) {
    fun registerCommand(command: LiteralCommandNode<CommandSource>) {
        val meta =
            commandManager
                .metaBuilder(BrigadierCommand(command))
                .plugin(velocityCore)
                .build()

        commandManager.register(meta, BrigadierCommand(command))
    }
}
