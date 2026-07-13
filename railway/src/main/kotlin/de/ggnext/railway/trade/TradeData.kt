package de.ggnext.railway.trade

import com.noxcrew.interfaces.properties.InterfaceProperty
import com.noxcrew.interfaces.properties.interfaceProperty
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class TradeSession(
    val player1: TradePlayer,
    val player2: TradePlayer,
    var lastChanged: Long = System.currentTimeMillis(),
) {
    val offerChangedProperty =
        interfaceProperty(0)

    fun triggerOfferUpdate() {
        player1.accepted.value = false

        player2.accepted.value = false

        lastChanged =
            System.currentTimeMillis()

        offerChangedProperty.value += 1
    }
}

data class TradePlayer(
    val playerUUID: UUID,
    val offer: MutableList<ItemStack>,
    var accepted: InterfaceProperty<Boolean>,
)

data class TradeRequest(
    val sender: UUID,
    val target: UUID,
    val expiration: Long,
)
