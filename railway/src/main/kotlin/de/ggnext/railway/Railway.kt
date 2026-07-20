package de.ggnext.railway

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import de.ggnext.core.api.GGNextAPI
import de.ggnext.railway.auction.AuctionCommand
import de.ggnext.railway.auction.AuctionGui
import de.ggnext.railway.auction.AuctionManager
import de.ggnext.railway.profile.ProfileCommand
import de.ggnext.railway.profile.ProfileGui
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
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class Railway : SuspendingJavaPlugin() {
    lateinit var zoneManager: ZoneManager
    lateinit var railwayProfileManager: RailwayProfileManager
    lateinit var profileGui: ProfileGui
    lateinit var tradeManager: TradeManager
    lateinit var tradeGui: TradeGui
    lateinit var auctionManager: AuctionManager
    lateinit var auctionGui: AuctionGui

    private var auctionExpirationTask: BukkitTask? = null

    override suspend fun onEnableAsync() {
        val zoneConfigs = ZoneConfigLoader(this).load()
        railwayProfileManager = RailwayProfileManager(GGNextAPI.mongoManager)
        auctionManager = AuctionManager(GGNextAPI.mongoManager)
        auctionGui = AuctionGui(this, auctionManager, railwayProfileManager)
        profileGui = ProfileGui(this, railwayProfileManager)
        zoneManager = ZoneManager(this, zoneConfigs, railwayProfileManager)
        tradeManager = TradeManager()
        tradeGui = TradeGui(this, tradeManager)
        registerCommands()
        registerListeners()
        scheduleAuctionExpiration()
    }

    override suspend fun onDisableAsync() {
        auctionExpirationTask?.cancel()

        for (player in Bukkit.getOnlinePlayers()) {
            val session =
                tradeManager.getSession(player.uniqueId)
                    ?: continue

            tradeManager.endSession(
                session,
            )
        }
        // Plugin shutdown logic
    }

    private fun scheduleAuctionExpiration() {
        auctionExpirationTask =
            server.scheduler.runTaskTimerAsynchronously(
                this,
                Runnable {
                    this@Railway.launch {
                        auctionManager.expireAuctions()
                    }
                },
                0L,
                AUCTION_EXPIRATION_CHECK_INTERVAL,
            )
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(ZoneSCommand(this, zoneManager).command)
            commands.register(ZoneCCommand(this, zoneManager).command)
            commands.register(ProfileCommand(railwayProfileManager, profileGui, this).command)
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

    companion object {
        private const val AUCTION_EXPIRATION_CHECK_INTERVAL = 20L * 60L * 10L
    }
}
