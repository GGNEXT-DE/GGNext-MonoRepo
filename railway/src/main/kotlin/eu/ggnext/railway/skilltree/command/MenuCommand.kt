package eu.ggnext.railway.skilltree.command

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import eu.ggnext.railway.skilltree.gui.MenuGui
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class MenuCommand(
    private val menuGui: MenuGui,
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("menu")
            .requires { it.sender is Player }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                plugin.launch {
                    menuGui.openMenuGui(player)
                }
                Command.SINGLE_SUCCESS
            }.build()
}
