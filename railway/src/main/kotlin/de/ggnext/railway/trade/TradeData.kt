package de.ggnext.railway.trade

import com.noxcrew.interfaces.properties.interfaceProperty
import de.ggnext.railway.profile.RailwayProfile
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class TradeSession(
    val player1: TradePlayer,
    val player2: TradePlayer,
    var lastChanged: Long = System.currentTimeMillis(),
) {
    val accepted1Property =
        interfaceProperty(false)

    val accepted2Property =
        interfaceProperty(false)

    val offerChangedProperty =
        interfaceProperty(0)

    var accepted1 by accepted1Property

    var accepted2 by accepted2Property

    fun triggerOfferUpdate() {
        player1.accept =
            false

        player2.accept =
            false

        accepted1 =
            false

        accepted2 =
            false

        lastChanged =
            System.currentTimeMillis()

        offerChangedProperty.value += 1
    }

    fun triggerMoneyUpdate() {
        player1.accept =
            false

        player2.accept =
            false

        accepted1 =
            false

        accepted2 =
            false

        lastChanged =
            System.currentTimeMillis()
    }
}

data class TradePlayer(
    val player: Player,
    val offer: MutableList<ItemStack>,
    var money: Double,
    var accept: Boolean,
    val profile: RailwayProfile,
)

data class TradeRequest(
    val sender: Player,
    val target: Player,
    val expiration: Long,
)
