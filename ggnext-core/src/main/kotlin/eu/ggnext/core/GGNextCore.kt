package eu.ggnext.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.scope
import com.noxcrew.interfaces.InterfacesListeners
import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.economy.EconomyService
import eu.ggnext.common.job.JobManager
import eu.ggnext.common.logging.LogControl
import eu.ggnext.common.logging.LogLevel
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import eu.ggnext.common.sentry.SentryBuilder
import eu.ggnext.common.sentry.SentryConfig
import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.core.api.GGNextAPI
import eu.ggnext.core.command.CommandVisibilityFilter
import eu.ggnext.core.input.PlayerInputListener
import eu.ggnext.core.input.PlayerInputManager
import eu.ggnext.core.scoreboard.ScoreBoardListener
import eu.ggnext.core.scoreboard.ScoreBoardManager
import eu.ggnext.core.tab.TabListener
import eu.ggnext.core.tab.TabManager
import eu.ggnext.core.vanish.VanishCommand
import eu.ggnext.core.vanish.VanishManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary

class GGNextCore : SuspendingJavaPlugin() {
    private lateinit var mongoManager: MongoManager
    private lateinit var economyService: EconomyService
    private lateinit var contentSystem: ContentSystem
    private lateinit var scoreboardLibrary: ScoreboardLibrary
    private lateinit var scoreBoardManager: ScoreBoardManager
    private lateinit var playerInputManager: PlayerInputManager
    private lateinit var tabManager: TabManager
    private lateinit var jobManager: JobManager

    override suspend fun onEnableAsync() {
        saveDefaultConfig()
        if (config.getBoolean("prod")) LogControl.setLevel(LogLevel.INFO) else LogControl.setLevel(LogLevel.DEBUG)

        SentryBuilder.init(
            SentryConfig(
                config.getString("sentry.dsn"),
                config.getBoolean("prod"),
            ),
        )

        InterfacesListeners.install(this)

        mongoManager = MongoManager(config.getString("mongo.connectionString"), config.getString("mongo.database"))
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
            log.warn("Could not load scoreboard library")
        }

        scoreBoardManager = ScoreBoardManager(scoreboardLibrary)
        GGNextAPI.scoreBoardManager = scoreBoardManager

        server.pluginManager.registerEvents(ScoreBoardListener(scoreBoardManager), this)

        playerInputManager = PlayerInputManager(this)
        GGNextAPI.playerInputManager = playerInputManager

        server.pluginManager.registerEvents(PlayerInputListener(this, playerInputManager), this)

        tabManager = TabManager()
        server.pluginManager.registerEvents(TabListener(tabManager), this)

        server.pluginManager.registerEvents(CommandVisibilityFilter(config.getBoolean("prod")), this)

        jobManager = JobManager(scope, logger).also { it.startAll() }
        GGNextAPI.jobManager = jobManager

        log.info("GGNext Core enabled!")
    }

    override suspend fun onDisableAsync() {
        contentSystem.shutdown()
        mongoManager.close()
        scoreBoardManager.shutdown()
        playerInputManager.shutdown()

        log.info("GGNext Core disabled!")
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(VanishCommand(VanishManager(this)).command)
        }
    }
}
