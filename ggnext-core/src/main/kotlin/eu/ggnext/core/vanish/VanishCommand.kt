package eu.ggnext.core.vanish

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

class VanishCommand(
    private val manager: VanishManager,
) {
    val command =
        Commands
            .literal("toggle-vanish")
            .requires { it.sender is Player && it.sender.hasPermission("ggnext.paper.vanish") }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                manager.toggle(player)
                Command.SINGLE_SUCCESS
            }.build()
}
