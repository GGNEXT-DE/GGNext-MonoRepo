package eu.ggnext.railway

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import eu.ggnext.core.api.GGNextAPI
import eu.ggnext.railway.auction.AuctionCommand
import eu.ggnext.railway.auction.AuctionGui
import eu.ggnext.railway.auction.AuctionJob
import eu.ggnext.railway.auction.AuctionManager
import eu.ggnext.railway.profile.ProfileCommand
import eu.ggnext.railway.profile.ProfileGui
import eu.ggnext.railway.profile.ProfileListener
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.skilltree.SkillTreeManager
import eu.ggnext.railway.skilltree.effect.EffectManager
import eu.ggnext.railway.trade.TradeCommand
import eu.ggnext.railway.trade.TradeGui
import eu.ggnext.railway.trade.TradeListener
import eu.ggnext.railway.trade.TradeManager
import eu.ggnext.railway.zone.ZoneManager
import eu.ggnext.railway.zone.command.ZoneCCommand
import eu.ggnext.railway.zone.command.ZoneSCommand
import eu.ggnext.railway.zone.config.ZoneConfigLoader
import eu.ggnext.railway.zone.listener.ZoneListener
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
    lateinit var effectManager: EffectManager

    override suspend fun onEnableAsync() {
        val zoneConfigs =
            ZoneConfigLoader(this)
                .load()
        railwayProfileManager =
            RailwayProfileManager(GGNextAPI.mongoManager)
        auctionManager =
            AuctionManager(GGNextAPI.mongoManager)
        auctionGui =
            AuctionGui(this, auctionManager, railwayProfileManager)
        profileGui =
            ProfileGui(this, railwayProfileManager)
        zoneManager =
            ZoneManager(this, zoneConfigs, railwayProfileManager)
        tradeManager =
            TradeManager()
        tradeGui =
            TradeGui(this, tradeManager)
        skillTreeManager =
            SkillTreeManager(railwayProfileManager)
        effectManager =
            EffectManager()
        registerCommands()
        registerListeners()

        GGNextAPI.jobManager.registerJob(
            AuctionJob(auctionManager),
        )
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
