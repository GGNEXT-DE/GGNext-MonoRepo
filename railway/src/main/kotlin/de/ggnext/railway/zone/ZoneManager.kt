package de.ggnext.railway.zone

import de.ggnext.railway.profile.RailwayProfileManager
import de.ggnext.railway.zone.config.ConfigPositionType
import de.ggnext.railway.zone.config.ZoneConfig
import de.ggnext.railway.zone.instance.ZoneInstanceManager
import de.ggnext.railway.zone.instance.ZoneLoader
import de.ggnext.railway.zone.session.ZoneSession
import de.ggnext.railway.zone.session.ZoneSessionManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ZoneManager(
    private val plugin: JavaPlugin,
    private val zoneConfigs: List<ZoneConfig>,
    private val railwayProfileManager: RailwayProfileManager,
) {
    private val instanceManager = ZoneInstanceManager()
    private val sessionManager = ZoneSessionManager(railwayProfileManager)
    private val zoneLoader = ZoneLoader(plugin)

    suspend fun startRun(
        player: Player,
        zoneId: String,
    ): ZoneSession {
        val zone = zoneConfigs.firstOrNull { it.name == zoneId } ?: error("Zone $zoneId does not exist")
        val spawnData =
            zone.markers.firstOrNull { it.type == ConfigPositionType.SPAWN } ?: error("Zone $zoneId does not have a spawn location")
        val spawnLocation = Location(player.world, spawnData.x, spawnData.y, spawnData.z)

        val slot = instanceManager.allocateSlot()
        val session = sessionManager.addSession(player, zoneId, slot)

        zoneLoader.spawnSlot(slot, zone)

        player.teleport(spawnLocation)

        return session
    }

    suspend fun stopRun(player: Player) {
        val session = sessionManager.getSession(player) ?: return

        zoneLoader.cleanSlot(session.slot)
        instanceManager.freeSlot(session.slot)

        sessionManager.deleteSession(player)
    }
}
