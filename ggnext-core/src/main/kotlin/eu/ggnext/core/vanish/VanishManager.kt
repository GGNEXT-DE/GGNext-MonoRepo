package eu.ggnext.core.vanish

import eu.ggnext.core.GGNextCore
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class VanishManager(
    private val plugin: GGNextCore,
) {
    private val vanishedPlayers = mutableSetOf<UUID>()

    fun vanish(player: Player) = setVanished(player, hidden = true)

    fun show(player: Player) = setVanished(player, hidden = false)

    fun toggle(player: Player) {
        if (isVanished(player)) show(player) else vanish(player)
    }

    fun isVanished(player: Player): Boolean = vanishedPlayers.contains(player.uniqueId)

    /** Hide all currently vanished players from a player who just joined. */
    fun applyVanishState(joiningPlayer: Player) {
        vanishedPlayers.forEach { vanishedId ->
            Bukkit.getPlayer(vanishedId)?.let { joiningPlayer.hidePlayer(plugin, it) }
        }
    }

    /** Clear vanish state on quit so a reconnect doesn't invert the next toggle. */
    fun clearOnQuit(player: Player) {
        vanishedPlayers.remove(player.uniqueId)
    }

    private fun setVanished(
        player: Player,
        hidden: Boolean,
    ) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == player) continue
            if (hidden) onlinePlayer.hidePlayer(plugin, player) else onlinePlayer.showPlayer(plugin, player)
        }
        if (hidden) vanishedPlayers.add(player.uniqueId) else vanishedPlayers.remove(player.uniqueId)
    }
}
