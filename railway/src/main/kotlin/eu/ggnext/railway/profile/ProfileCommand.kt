package eu.ggnext.railway.profile

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ProfileCommand(
    private val profileGui: ProfileGui,
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("profile")
            .requires { it.sender is Player }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                plugin.launch {
                    profileGui.openProfileGui(player)
                }
                Command.SINGLE_SUCCESS
            }.build()
}
