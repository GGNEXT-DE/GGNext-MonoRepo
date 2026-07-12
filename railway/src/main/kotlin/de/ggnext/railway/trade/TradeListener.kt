package de.ggnext.railway.trade

import com.github.shynixn.mccoroutine.bukkit.launch
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.language
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.java.JavaPlugin

class TradeListener(
    private val plugin: JavaPlugin,
    private val tradeManager: TradeManager,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
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

        val player =
            event.whoClicked as? Player
                ?: return

        val session =
            tradeManager.getSession(player)
                ?: return

        if (event.clickedInventory == null) {
            return
        }

        if (event.clickedInventory!!.type != InventoryType.PLAYER) {
            event.isCancelled = true
            return
        }

        val item =
            event.currentItem
                ?: return

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
            event.isCancelled = true
            return
        }

        val tradePlayer =
            if (session.player1.player == player) {
                session.player1
            } else {
                session.player2
            }

        if (tradePlayer.offer.size >= 12) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true

        tradePlayer.offer.add(
            item.clone(),
        )

        event.clickedInventory!!.setItem(
            event.slot,
            null,
        )

        session.triggerOfferUpdate()
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player =
            event.whoClicked as? Player
                ?: return

        if (
            tradeManager.getSession(player)
            != null
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player =
            event.player as? Player
                ?: return

        val succeedSession = tradeManager.getSucceedSession(player)
        if (succeedSession != null) {
            tradeManager.removeSucceedSession(succeedSession)
            return
        }

        val session =
            tradeManager.getSession(player)
                ?: return

        tradeManager.endSession(
            session,
        )

        session.player1.offer.forEach {
            session.player1.player.inventory.addItem(
                it.clone(),
            )
        }

        session.player2.offer.forEach {
            session.player2.player.inventory.addItem(
                it.clone(),
            )
        }

        session.player1.offer.clear()
        session.player2.offer.clear()

        val cancelled by TranslationStore(
            "translations.railway.trade.cancelled",
        )

        val p1 =
            session.player1.player

        val p2 =
            session.player2.player

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
}
