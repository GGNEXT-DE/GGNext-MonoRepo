package eu.ggnext.railway.trade

import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.toPlayer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType

class TradeListener(
    private val tradeManager: TradeManager,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.clickedInventory == null) {
            return
        }

        val player = event.whoClicked.toPlayer() ?: return

        val session = tradeManager.getSession(player.uniqueId) ?: return

        if (event.clickedInventory?.type != InventoryType.PLAYER) {
            event.isCancelled = true
            return
        }

        when (event.action) {
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT,
            InventoryAction.DROP_ALL_CURSOR,
            InventoryAction.DROP_ONE_CURSOR,
            -> {
                event.isCancelled = true
                return
            }

            else -> {}
        }

        val item = event.currentItem ?: return

        if (item.type.isAir) {
            return
        }

        if (
            item.type == Material.GREEN_WOOL ||
            item.type == Material.RED_WOOL ||
            item.type == Material.LIME_STAINED_GLASS_PANE ||
            item.type == Material.RED_STAINED_GLASS_PANE ||
            item.type == Material.BLACK_STAINED_GLASS_PANE ||
            item.type == Material.GRAY_STAINED_GLASS_PANE
        ) {
            if (event.clickedInventory == event.view.topInventory) {
                player.sendMessage(event.clickedInventory.toString())
                event.isCancelled = true
                return
            }
        }

        val tradePlayer =
            if (session.player1.playerUUID.toPlayer() == player) {
                session.player1
            } else {
                session.player2
            }

        if (tradePlayer.offer.size >= 16) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true

        tradePlayer.offer.add(
            item.clone(),
        )

        event.clickedInventory?.setItem(
            event.slot,
            null,
        )

        session.triggerOfferUpdate()
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked.toPlayer() ?: return

        if (tradeManager.getSession(player.uniqueId) != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player.toPlayer() ?: return

        val session =
            tradeManager.getSession(player.uniqueId)
                ?: return

        tradeManager.endSession(
            session,
        )

        val cancelled by TranslationStore(
            "translations.railway.trade.cancelled",
        )

        val p1 =
            session.player1.playerUUID.toPlayer() ?: return

        val p2 =
            session.player2.playerUUID.toPlayer() ?: return

        p1.sendMessage(
            cancelled.get(
                p1.language(),
            ),
        )

        p2.sendMessage(
            cancelled.get(
                p2.language(),
            ),
        )

        if (player == p1) {
            p2.closeInventory()
        } else {
            p1.closeInventory()
        }
    }

    @EventHandler
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return

        if (tradeManager.getSession(player.uniqueId) != null) {
            event.isCancelled = true
        }
    }
}
