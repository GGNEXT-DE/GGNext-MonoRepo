package eu.ggnext.railway.skilltree.listener

import eu.ggnext.railway.skilltree.gui.SkillPathDetailGui
import eu.ggnext.railway.skilltree.gui.SkilltreeOverviewGui
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent

class SkilltreeListener(
    private val skilltreeOverviewGui: SkilltreeOverviewGui,
    private val skillPathDetailGui: SkillPathDetailGui,
) : Listener {
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (skilltreeOverviewGui.consumeNavigating(player)) {
            return
        }

        if (skillPathDetailGui.consumeNavigating(player)) {
            return
        }
    }
}
