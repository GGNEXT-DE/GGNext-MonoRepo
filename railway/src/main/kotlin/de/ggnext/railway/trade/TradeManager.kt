package de.ggnext.railway.trade

import de.ggnext.railway.profile.RailwayProfileManager
import org.bukkit.entity.Player
import java.util.UUID

class TradeManager(
    private val profileManager: RailwayProfileManager,
) {
    private val activeSessions = mutableMapOf<UUID, TradeSession>()
    private val activeRequests = mutableListOf<TradeRequest>()
    private val succeedSession = mutableMapOf<UUID, TradeSession>()

    fun createRequest(
        sender: Player,
        target: Player,
    ) {
        removeRequest(sender, target)

        val expirationTime = System.currentTimeMillis() + (5 * 60 * 1000L)
        activeRequests.add(TradeRequest(sender, target, expirationTime))
    }

    fun removeRequest(
        sender: Player,
        target: Player,
    ) {
        activeRequests.removeIf { it.sender == sender && it.target == target }
    }

    fun getFirstValidRequestForTarget(target: Player): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.target == target }
    }

    fun getValidRequestFromSender(
        sender: Player,
        target: Player,
    ): TradeRequest? {
        cleanupRequests()
        return activeRequests.find { it.sender == sender && it.target == target }
    }

    fun cleanupRequests() {
        val now = System.currentTimeMillis()
        activeRequests.removeIf { now > it.expiration }
    }

    fun startSession(
        sender: Player,
        target: Player,
    ): TradeSession {
        activeRequests.removeIf { it.sender == sender && it.target == target }

        val senderProfile = profileManager.getActiveProfile(sender) ?: throw IllegalStateException("Sender hat kein aktives Profil!")
        val targetProfile = profileManager.getActiveProfile(target) ?: throw IllegalStateException("Target hat kein aktives Profil!")

        val tradePlayer1 = TradePlayer(sender, mutableListOf(), 0.0, false, senderProfile)
        val tradePlayer2 = TradePlayer(target, mutableListOf(), 0.0, false, targetProfile)
        val session = TradeSession(tradePlayer1, tradePlayer2)

        activeSessions[sender.uniqueId] = session
        activeSessions[target.uniqueId] = session

        return session
    }

    fun endSession(session: TradeSession) {
        activeSessions.remove(session.player1.player.uniqueId)
        activeSessions.remove(session.player2.player.uniqueId)

        succeedSession[session.player1.player.uniqueId] = session
        succeedSession[session.player2.player.uniqueId] = session
    }

    fun removeSucceedSession(session: TradeSession) {
        succeedSession.remove(session.player1.player.uniqueId)
        succeedSession.remove(session.player2.player.uniqueId)
    }

    fun getSession(player: Player): TradeSession? = activeSessions[player.uniqueId]

    fun getSucceedSession(player: Player): TradeSession? = succeedSession[player.uniqueId]
}
