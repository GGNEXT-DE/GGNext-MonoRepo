package de.ggnext.core.tab

import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.language
import org.bukkit.entity.Player

class TabManager {
    fun setTab(player: Player) {
        val tabHeader by TranslationStore("translations.core.tab.header")
        val tabFooter by TranslationStore("translations.core.tab.footer")

        player.sendPlayerListHeaderAndFooter(
            tabHeader.get(player.language()),
            tabFooter.get(player.language()),
        )
    }
}
