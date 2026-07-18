package de.ggnext.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.scope
import com.noxcrew.interfaces.InterfacesListeners
import de.ggnext.contentsystem.ContentSystem
import de.ggnext.core.api.GGNextAPI
import de.ggnext.core.command.CommandVisibilityFilter
import de.ggnext.core.db.MongoManager
import de.ggnext.core.economy.EconomyService
import de.ggnext.core.scoreboard.ScoreBoardListener
import de.ggnext.core.scoreboard.ScoreBoardManager
import de.ggnext.core.tab.TabListener
import de.ggnext.core.tab.TabManager
import de.ggnext.core.vanish.VanishCommand
import de.ggnext.core.vanish.VanishManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary

class GGNextCore : SuspendingJavaPlugin() {
    private lateinit var mongoManager: MongoManager
    private lateinit var economyService: EconomyService
    private lateinit var contentSystem: ContentSystem
    private lateinit var scoreboardLibrary: ScoreboardLibrary
    private lateinit var scoreBoardManager: ScoreBoardManager
    private lateinit var tabManager: TabManager

    override suspend fun onEnableAsync() {
        saveDefaultConfig()

        InterfacesListeners.install(this)

        mongoManager = MongoManager(config)
        GGNextAPI.mongoManager = mongoManager

        registerCommands()
        contentSystem = ContentSystem(mongoManager.database, scope).also { it.init() }

        economyService = EconomyService(mongoManager.database)
        GGNextAPI.economyService = economyService

        runCatching {
            System.setProperty("net.megavex.scoreboardlibrary.forceModern", "true")
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this)
        }.onFailure {
            scoreboardLibrary = NoopScoreboardLibrary()
            logger.warning("Could not load scoreboard library")
        }

        scoreBoardManager = ScoreBoardManager(scoreboardLibrary)
        GGNextAPI.scoreBoardManager = scoreBoardManager

        server.pluginManager.registerEvents(ScoreBoardListener(scoreBoardManager), this)

        tabManager = TabManager()
        server.pluginManager.registerEvents(TabListener(tabManager), this)

        server.pluginManager.registerEvents(CommandVisibilityFilter(config.getBoolean("prod")), this)

        logger.info("GGNext Core enabled!")
    }

    override suspend fun onDisableAsync() {
        contentSystem.shutdown()
        mongoManager.close()
        scoreBoardManager.shutdown()

        logger.info("GGNext Core disabled!")
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(VanishCommand(VanishManager(this)).command)
        }
    }
}
