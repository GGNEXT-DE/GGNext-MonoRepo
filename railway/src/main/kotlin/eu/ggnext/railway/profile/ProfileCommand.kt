package eu.ggnext.railway.profile

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.Command
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.api.GGNextAPI
import eu.ggnext.core.utils.language
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ProfileCommand(
    private val profileGui: ProfileGui,
    private val profileManager: RailwayProfileManager,
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
                Commands.literal("rename").executes { ctx ->
                    val player = ctx.source.sender as Player
                    plugin.launch {
                        requestRename(player)
                    }
                    Command.SINGLE_SUCCESS
                },
            ).build()

    private suspend fun requestRename(player: Player) {
        if (profileManager.getActiveProfileId(player) == null) {
            val noActiveProfileKey by TranslationStore("translations.railway.profile.rename.no_active_profile")
            player.sendMessage(noActiveProfileKey.get(player.language()))
            return
        }

        GGNextAPI.playerInputManager.requestInput(
            player = player,
            callback = { newName ->
                val successKey by TranslationStore("translations.railway.profile.rename.success")
                val errorKey by TranslationStore("translations.railway.profile.rename.error")

                val renamedProfile = profileManager.renameActiveProfile(player, newName)
                if (renamedProfile != null) {
                    player.sendMessage(successKey.get(player.language()))
                } else {
                    player.sendMessage(errorKey.get(player.language()))
                }
            },
        )
    }
}
