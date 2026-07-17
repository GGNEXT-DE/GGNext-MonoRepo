package de.ggnext.core.tab

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class TabManager {
    fun setTab(player: Player) {
        player.sendPlayerListHeaderAndFooter(
            Component.text("GGNext", NamedTextColor.GREEN),
            Component.text("play.ggnext.de", NamedTextColor.AQUA),
        )
    }
}
