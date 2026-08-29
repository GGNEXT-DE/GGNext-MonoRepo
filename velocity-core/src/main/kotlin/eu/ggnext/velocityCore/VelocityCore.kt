package eu.ggnext.velocityCore

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.github.shynixn.mccoroutine.velocity.scope
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.economy.EconomyService
import eu.ggnext.common.logging.LogControl
import eu.ggnext.common.logging.LogLevel
import eu.ggnext.common.sentry.SentryBuilder
import eu.ggnext.common.sentry.SentryConfig
import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.velocityCore.config.ConfigManager
import eu.ggnext.velocityCore.config.VelocityConfig
import eu.ggnext.velocityCore.features.friends.FriendSystemManager
import eu.ggnext.velocityCore.features.friends.commands.FriendCommand
import eu.ggnext.velocityCore.features.hub.HubCommand
import eu.ggnext.velocityCore.features.maintenance.commands.EditMtCommand
import eu.ggnext.velocityCore.features.maintenance.commands.ToggleMtCommand
import eu.ggnext.velocityCore.features.maintenance.listener.MaintenanceListener
import eu.ggnext.velocityCore.features.party.PartySystemManager
import eu.ggnext.velocityCore.features.party.commands.PartyCommand
import eu.ggnext.velocityCore.features.players.PlayerManager
import eu.ggnext.velocityCore.features.players.listener.PlayerListener
import eu.ggnext.velocityCore.features.punishment.PunishmentChatListener
import eu.ggnext.velocityCore.features.punishment.PunishmentManager
import eu.ggnext.velocityCore.features.punishment.commands.BanCommands
import eu.ggnext.velocityCore.features.punishment.commands.HistoryCommand
import eu.ggnext.velocityCore.features.punishment.commands.MuteCommands
import eu.ggnext.velocityCore.features.punishment.commands.WarnCommand
import eu.ggnext.velocityCore.features.team.TeamChatListener
import eu.ggnext.velocityCore.features.verify.VerifyCommand
import eu.ggnext.velocityCore.features.verify.VerifyManager
import eu.ggnext.velocityCore.features.vote.VoteCommand
import eu.ggnext.velocityCore.features.vote.VoteRewardListener
import eu.ggnext.velocityCore.utils.CommandRegistry
import eu.ggnext.velocityCore.utils.CommandUtils
import java.nio.file.Path

@Plugin(
    id = "velocity-core",
    name = "Velocity-Core",
    version = "1.0-SNAPSHOT",
    dependencies = [Dependency(id = "nuvotifier")],
)
class VelocityCore
    @Inject
    constructor(
        private val suspendingPluginContainer: SuspendingPluginContainer,
        private val server: ProxyServer,
        @DataDirectory private val dataFolder: Path,
    ) {
        init {
            suspendingPluginContainer.initialize(this)
        }

        private val scope by lazy { suspendingPluginContainer.pluginContainer.scope }

        val configManager = ConfigManager(dataFolder)
        private lateinit var config: VelocityConfig
        private lateinit var mongoManager: MongoManager
        private lateinit var playerManager: PlayerManager
        private lateinit var punishmentManager: PunishmentManager
        private lateinit var commandUtils: CommandUtils
        private lateinit var verifyManager: VerifyManager
        private lateinit var partySystemManager: PartySystemManager
        private lateinit var friendSystemManager: FriendSystemManager
        private lateinit var economyService: EconomyService

        @Subscribe
        suspend fun onProxyInitialization(event: ProxyInitializeEvent) {
            configManager.load()
            configManager.save()
            config = configManager.config
            if (config.prod) LogControl.setLevel(LogLevel.INFO) else LogControl.setLevel(LogLevel.DEBUG)

            SentryBuilder.init(
                SentryConfig(
                    config.sentryDSN,
                    config.prod,
                ),
            )

            mongoManager = MongoManager(config.mongo.connection, config.mongo.database)
            playerManager = PlayerManager(mongoManager.database)
            punishmentManager = PunishmentManager(mongoManager.database)
            commandUtils = CommandUtils(server, playerManager)
            verifyManager = VerifyManager(mongoManager.database)
            partySystemManager = PartySystemManager(playerManager)
            friendSystemManager = FriendSystemManager(playerManager)
            economyService = EconomyService(mongoManager.database)

            registerCommands()

            server.eventManager.register(this, TeamChatListener(server))
            server.eventManager.registerSuspend(
                this,
                PlayerListener(playerManager, punishmentManager, partySystemManager, config),
            )
            server.eventManager.registerSuspend(this, PunishmentChatListener(punishmentManager))
            server.eventManager.register(this, MaintenanceListener(configManager.config))

            ContentSystem(mongoManager.database, scope).also { it.init() }
            server.eventManager.registerSuspend(this, VoteRewardListener(server, playerManager, economyService))
        }

        private fun registerCommands() {
            val commandRegistry = CommandRegistry(server.commandManager, this)

            commandRegistry.registerCommand(HubCommand(server).command)
            commandRegistry.registerCommand(ToggleMtCommand(configManager).command)
            commandRegistry.registerCommand(EditMtCommand(configManager).command)

            val banCommands = BanCommands(commandUtils, punishmentManager, scope, server)
            commandRegistry.registerCommand(banCommands.ban)
            commandRegistry.registerCommand(banCommands.unBan)
            commandRegistry.registerCommand(banCommands.tempBan)

            commandRegistry.registerCommand(HistoryCommand(commandUtils, punishmentManager, server, scope).history)

            val muteCommands = MuteCommands(server, commandUtils, punishmentManager, scope)
            commandRegistry.registerCommand(muteCommands.mute)
            commandRegistry.registerCommand(muteCommands.unMute)
            commandRegistry.registerCommand(muteCommands.tempMute)

            commandRegistry.registerCommand(WarnCommand(server, commandUtils, punishmentManager, scope).warn)

            commandRegistry.registerCommand(VerifyCommand(verifyManager, playerManager, scope).command)

            commandRegistry.registerCommand(VoteCommand().command)

            commandRegistry.registerCommand(PartyCommand(partySystemManager, commandUtils, server, scope).command)

            commandRegistry.registerCommand(FriendCommand(friendSystemManager, commandUtils, server, scope).command)
        }
    }
