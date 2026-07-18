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
            }.then(
                Commands
                    .literal("create")
                    .then(
                        stringArgument("name")
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val name = ctx.getArgument("name", String::class.java)

                                plugin.launch {
                                    val profile = railwayProfileManager.createProfile(player, name)
                                    if (profile != null) {
                                        val msg by TranslationStore("translations.railway.profile.create.success")
                                        player.sendMessage(msg.get(player.language()))
                                    } else {
                                        val msg by TranslationStore("translations.railway.profile.create.failed")
                                        player.sendMessage(msg.get(player.language()))
                                    }
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                Commands
                    .literal("delete")
                    .then(
                        stringArgument("name")
                            .suggests { ctx, builder ->
                                val future = CompletableFuture<Suggestions>()
                                plugin.launch {
                                    val profileNames = railwayProfileManager.getProfiles(ctx.source.sender as Player).map { it.name }
                                    profileNames.forEach { builder.suggest(it.lowercase()) }
                                    future.complete(builder.build())
                                }
                                future
                            }.executes { ctx ->
                                val player = ctx.source.sender as Player
                                val name = ctx.getArgument("name", String::class.java)

                                plugin.launch {
                                    val profile = railwayProfileManager.getProfileByName(player, name)
                                    if (profile != null) {
                                        railwayProfileManager.deleteProfile(player, profile.id)
                                        val msg by TranslationStore("translations.railway.profile.delete.success")
                                        player.sendMessage(msg.get(player.language()))
                                    } else {
                                        val msg by TranslationStore("translations.railway.profile.delete.failed")
                                        player.sendMessage(msg.get(player.language()))
                                    }
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                Commands
                    .literal("select")
                    .then(
                        stringArgument("name")
                            .suggests { ctx, builder ->
                                val future = CompletableFuture<Suggestions>()
                                plugin.launch {
                                    val profileNames = railwayProfileManager.getProfiles(ctx.source.sender as Player).map { it.name }
                                    profileNames.forEach { builder.suggest(it.lowercase()) }
                                    future.complete(builder.build())
                                }
                                future
                            }.executes { ctx ->
                                val player = ctx.source.sender as Player
                                val name = ctx.getArgument("name", String::class.java)

                                plugin.launch {
                                    val profile = railwayProfileManager.getProfileByName(player, name)
                                    if (profile != null) {
                                        railwayProfileManager.setActiveProfile(player, profile)
                                        val msg by TranslationStore("translations.railway.profile.select.success")
                                        player.sendMessage(msg.get(player.language()))
                                    } else {
                                        val msg by TranslationStore("translations.railway.profile.select.failed")
                                        player.sendMessage(msg.get(player.language()))
                                    }
                                }
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ).then(
                Commands
                    .literal("list")
                    .executes { ctx ->
                        val player = ctx.source.sender as Player

                        plugin.launch {
                            val profileNames = railwayProfileManager.getProfiles(player).map { it.name }

                            player.sendMessage(Component.text("Profiles: ${profileNames.joinToString()}"))
                        }

                        Command.SINGLE_SUCCESS
                    },
            ).build()
}
