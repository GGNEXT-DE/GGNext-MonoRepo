package de.ggnext.railway

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import de.ggnext.core.api.GGNextAPI
import de.ggnext.railway.auction.AuctionCommand
import de.ggnext.railway.auction.AuctionGui
import de.ggnext.railway.auction.AuctionJob
import de.ggnext.railway.auction.AuctionManager
import de.ggnext.railway.profile.ProfileCommand
import de.ggnext.railway.profile.ProfileGui
import de.ggnext.railway.profile.ProfileListener
import de.ggnext.railway.profile.RailwayProfileManager
import de.ggnext.railway.skilltree.SkillTreeManager
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
import org.bukkit.Bukkit

class Railway : SuspendingJavaPlugin() {
    lateinit var zoneManager: ZoneManager
    lateinit var railwayProfileManager: RailwayProfileManager
    lateinit var profileGui: ProfileGui
    lateinit var tradeManager: TradeManager
    lateinit var tradeGui: TradeGui
    lateinit var auctionManager: AuctionManager
    lateinit var auctionGui: AuctionGui
    lateinit var skillTreeManager: SkillTreeManager

    override suspend fun onEnableAsync() {
        val zoneConfigs = ZoneConfigLoader(this).load()
        railwayProfileManager = RailwayProfileManager(GGNextAPI.mongoManager)
        auctionManager = AuctionManager(GGNextAPI.mongoManager)
        auctionGui = AuctionGui(this, auctionManager, railwayProfileManager)
        profileGui = ProfileGui(this, railwayProfileManager)
        zoneManager = ZoneManager(this, zoneConfigs, railwayProfileManager)
        tradeManager = TradeManager()
        tradeGui = TradeGui(this, tradeManager)
        skillTreeManager = SkillTreeManager(railwayProfileManager)
        registerCommands()
        registerListeners()

        GGNextAPI.jobManager.registerJob(AuctionJob(auctionManager))
    }

    override suspend fun onDisableAsync() {
        for (player in Bukkit.getOnlinePlayers()) {
            val session =
                tradeManager.getSession(player.uniqueId)
                    ?: continue

            tradeManager.endSession(
                session,
            )
        }
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(ZoneSCommand(this, zoneManager).command)
            commands.register(ZoneCCommand(this, zoneManager).command)
            commands.register(ProfileCommand(profileGui, this).command)
            commands.register(TradeCommand(tradeManager, tradeGui).command)
            commands.register(
                AuctionCommand(this, auctionGui).command,
                "Opens the Auction House.",
                listOf("ah", "auction"),
            )
        }
    }

    private fun registerListeners() {
        server.pluginManager.apply {
            registerSuspendingEvents(ZoneListener(zoneManager), this@Railway)
            registerSuspendingEvents(ProfileListener(railwayProfileManager, profileGui), this@Railway)
            registerSuspendingEvents(TradeListener(tradeManager), this@Railway)
        }
    }
}
