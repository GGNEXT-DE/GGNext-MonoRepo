package eu.ggnext.railway.fuel.listener

import eu.ggnext.railway.fuel.gui.FuelGui
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent

class FuelListener(
    private val fuelGui: FuelGui,
) : Listener {
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        fuelGui.consumeNavigating(player)
    }
}
