package de.ggnext.railway

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import de.ggnext.core.api.GGNextAPI
import de.ggnext.railway.profile.ProfileCommand
import de.ggnext.railway.profile.ProfileListener
import de.ggnext.railway.profile.RailwayProfileManager
import de.ggnext.railway.trade.TradeCommand
import de.ggnext.railway.trade.TradeGui
import de.ggnext.railway.trade.TradeListener
import de.ggnext.railway.trade.TradeManager
import de.ggnext.railway.zone.ZoneManager
import de.ggnext.railway.zone.command.ZoneCCommand
import de.ggnext.railway.zone.command.ZoneSCommand
import de.ggnext.railway.zone.config.ZoneConfigLoader
import de.ggnext.railway.zone.listener.ZoneListener
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

class Railway : SuspendingJavaPlugin() {
    lateinit var zoneManager: ZoneManager
    lateinit var railwayProfileManager: RailwayProfileManager
    lateinit var tradeManager: TradeManager
    lateinit var tradeGui: TradeGui

    override suspend fun onEnableAsync() {
        val zoneConfigs = ZoneConfigLoader(this).load()
        railwayProfileManager = RailwayProfileManager(GGNextAPI.mongoManager)
        zoneManager = ZoneManager(this, zoneConfigs, railwayProfileManager)
        tradeManager = TradeManager(railwayProfileManager)
        tradeGui = TradeGui(this, tradeManager)
        registerCommands()
        registerListeners()
    }

    override suspend fun onDisableAsync() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(ZoneSCommand(this, zoneManager).command)
            commands.register(ZoneCCommand(this, zoneManager).command)
            commands.register(ProfileCommand(railwayProfileManager, this).command)
            commands.register(TradeCommand(tradeManager, tradeGui).command)
        }
    }

    private fun registerListeners() {
        server.pluginManager.apply {
            registerSuspendingEvents(ZoneListener(zoneManager), this@Railway)
            registerSuspendingEvents(ProfileListener(railwayProfileManager), this@Railway)
            registerSuspendingEvents(TradeListener(this@Railway, tradeManager), this@Railway)
        }
    }
}
