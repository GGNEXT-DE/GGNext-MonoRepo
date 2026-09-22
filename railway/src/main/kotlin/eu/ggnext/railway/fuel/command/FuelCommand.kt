package eu.ggnext.railway.fuel.command

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import eu.ggnext.railway.fuel.gui.FuelGui
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class FuelCommand(
    private val fuelGui: FuelGui,
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("fuel")
            .requires { it.sender is Player }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                plugin.launch {
                    fuelGui.openFuelGui(player)
                }
                Command.SINGLE_SUCCESS
            }.build()
}
