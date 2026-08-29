package eu.ggnext.railway.auction

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class AuctionCommand(
    private val plugin: JavaPlugin,
    private val auctionGui: AuctionGui,
) {
    val command =
        Commands
            .literal("auctionhouse")
            .requires { it.sender is Player }
            .executes { context ->
                val player = context.source.sender as Player

                plugin.launch {
                    auctionGui.openAuctionGui(player)
                }

                Command.SINGLE_SUCCESS
            }.build()
}
