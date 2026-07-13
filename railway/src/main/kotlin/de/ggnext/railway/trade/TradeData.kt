package de.ggnext.railway.trade

import com.noxcrew.interfaces.properties.interfaceProperty
import de.ggnext.railway.profile.RailwayProfile
import org.bukkit.inventory.ItemStack
import java.util.UUID

class TradeSession(
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
        player1.accept =
            false

        player2.accept =
            false

        accepted1.value = false

        accepted2.value = false

        lastChanged =
            System.currentTimeMillis()

        offerChangedProperty.value += 1
    }
}

data class TradePlayer(
    val player: UUID,
    val offer: MutableList<ItemStack>,
    var accept: Boolean,
)

data class TradeRequest(
    val sender: UUID,
    val target: UUID,
    val expiration: Long,
)
