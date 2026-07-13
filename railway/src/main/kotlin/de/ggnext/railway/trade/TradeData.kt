package de.ggnext.railway.trade

import com.noxcrew.interfaces.properties.interfaceProperty
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class TradeSession(
    val player1: TradePlayer,
    val player2: TradePlayer,
    var lastChanged: Long = System.currentTimeMillis(),
) {
    var accepted1 =
        interfaceProperty(false)

    var accepted2 =
        interfaceProperty(false)

    val offerChangedProperty =
        interfaceProperty(0)

    fun triggerOfferUpdate() {
        accepted1.value = false

        accepted2.value = false

        lastChanged =
            System.currentTimeMillis()

        offerChangedProperty.value += 1
    }
}

data class TradePlayer(
    val playerUUID: UUID,
    val offer: MutableList<ItemStack>,
)

data class TradeRequest(
    val sender: UUID,
    val target: UUID,
    val expiration: Long,
)
