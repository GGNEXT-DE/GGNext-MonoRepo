package eu.ggnext.railway.zone.session

import eu.ggnext.railway.profile.RailwayProfileManager
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ZoneSessionManager(
    private val railwayProfileManager: RailwayProfileManager,
) {
    private val sessions = ConcurrentHashMap<UUID, ZoneSession>()

    fun addSession(
        player: Player,
        zoneId: String,
        slot: Int,
    ): ZoneSession {
        val activeProfile =
            railwayProfileManager.getActiveProfile(player)
                ?: throw IllegalStateException("Player ${player.uniqueId} has no active profile when entering zone $zoneId")
        val session = ZoneSession(player.uniqueId, activeProfile.id, zoneId, slot)
        sessions[player.uniqueId] = session
        return session
    }

    fun getSession(player: Player) = sessions[player.uniqueId]

    fun deleteSession(player: Player) = sessions.remove(player.uniqueId)
}
