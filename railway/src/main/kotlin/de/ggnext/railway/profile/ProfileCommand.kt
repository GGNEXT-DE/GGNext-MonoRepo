package de.ggnext.railway.profile

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.Suggestions
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.language
import de.ggnext.core.utils.stringArgument
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture

class ProfileCommand(
    private val railwayProfileManager: RailwayProfileManager,
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
