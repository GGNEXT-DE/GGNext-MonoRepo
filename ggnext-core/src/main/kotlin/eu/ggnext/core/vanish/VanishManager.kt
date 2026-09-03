package eu.ggnext.core.vanish

import eu.ggnext.core.GGNextCore
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class VanishManager(
    private val plugin: GGNextCore,
) {
    val vanishedPlayer = mutableListOf<UUID>()

    fun vanish(player: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.hidePlayer(plugin, player)
            }
        }
        vanishedPlayer.add(player.uniqueId)
    }

    fun show(player: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.showPlayer(plugin, player)
            }
        }
        vanishedPlayer.remove(player.uniqueId)
    }

    fun toggle(player: Player) {
        if (vanishedPlayer.contains(player.uniqueId)) {
            vanish(player)
        }
        if (!vanishedPlayer.contains(player.uniqueId)) {
            show(player)
        }
    }
}
