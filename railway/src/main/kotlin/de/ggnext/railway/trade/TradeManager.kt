package de.ggnext.railway.trade

import com.noxcrew.interfaces.properties.InterfaceProperty
import de.ggnext.core.utils.toPlayer
import java.util.UUID

class TradeManager {
    private val activeSessions = mutableMapOf<UUID, TradeSession>()
    private val activeRequests = mutableListOf<TradeRequest>()

    fun createRequest(
        sender: UUID,
        target: UUID,
    ) {
        removeRequest(sender, target)

        val expirationTime = System.currentTimeMillis() + (5 * 60 * 1000L)
        activeRequests.add(TradeRequest(sender, target, expirationTime))
    }

    fun removeRequest(
        sender: UUID,
        target: UUID,
    ) {
        activeRequests.removeIf { it.sender == sender && it.target == target }
    }

    fun getFirstValidRequestForTarget(target: UUID): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.target == target }
    }

    fun getValidRequestFromSender(
        sender: UUID,
        target: UUID,
    ): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.sender == sender && it.target == target }
    }

    fun cleanupRequests() {
        val now = System.currentTimeMillis()
        activeRequests.removeIf { now > it.expiration }
    }

    fun startSession(
        sender: UUID,
        target: UUID,
    ): TradeSession {
        activeRequests.removeIf { it.sender == sender && it.target == target }

        val tradePlayer1 = TradePlayer(sender, mutableListOf(), InterfaceProperty(false))
        val tradePlayer2 = TradePlayer(target, mutableListOf(), InterfaceProperty(false))
        val session = TradeSession(tradePlayer1, tradePlayer2)

        activeSessions[sender] = session
        activeSessions[target] = session

        return session
    }

    fun endSession(session: TradeSession) {
        activeSessions.remove(session.player1.playerUUID)
        activeSessions.remove(session.player2.playerUUID)

        session.player1.offer.forEach {
            session.player1.playerUUID.toPlayer()?.inventory?.addItem(
                it.clone(),
            )
        }

        session.player2.offer.forEach {
            session.player2.playerUUID.toPlayer()?.inventory?.addItem(
                it.clone(),
            )
        }

        session.player1.offer.clear()
        session.player2.offer.clear()
    }

    fun getSession(player: UUID): TradeSession? = activeSessions[player]
}
