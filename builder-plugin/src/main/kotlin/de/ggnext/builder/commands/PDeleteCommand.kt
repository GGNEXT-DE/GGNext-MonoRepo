package de.ggnext.builder.commands

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin

class PDeleteCommand(
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("position-delete")
            .requires { it.sender is Player && it.sender.hasPermission("ggnext.position.delete") }
            .executes { ctx ->
                val player = ctx.source.sender as Player

                val entity =
                    player
                        .getNearbyEntities(3.0, 3.0, 3.0)
                        .filterIsInstance<TextDisplay>()
                        .filter { it.persistentDataContainer.has(NamespacedKey(plugin, "type")) }
                        .minByOrNull { it.location.distanceSquared(player.location) }

                if (entity == null) {
                    player.sendMessage(Component.text("No ConfigPosition nearby!", NamedTextColor.RED))
                    return@executes 0
                }

                entity.remove()
                player.sendMessage(Component.text("Deleted!", NamedTextColor.GREEN))
                Command.SINGLE_SUCCESS
            }.build()
}
