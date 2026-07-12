package de.ggnext.railway.zone.command

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import de.ggnext.railway.zone.ZoneManager
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ZoneSCommand(
    private val plugin: JavaPlugin,
    private val zoneManager: ZoneManager,
) {
    val command =
        Commands
            .literal("zone-start")
            .requires { it.sender is Player }
            .executes { ctx ->
                val player = ctx.source.sender as Player

                plugin.launch {
                    zoneManager.startRun(player, "1")
                }

                Command.SINGLE_SUCCESS
            }.build()
}
