package de.ggnext.velocityCore

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.github.shynixn.mccoroutine.velocity.scope
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.common.db.MongoManager
import de.ggnext.common.logging.LogControl
import de.ggnext.common.logging.LogLevel
import de.ggnext.common.sentry.SentryBuilder
import de.ggnext.common.sentry.SentryConfig
import de.ggnext.contentsystem.ContentSystem
import de.ggnext.velocityCore.config.ConfigManager
import de.ggnext.velocityCore.features.friends.FriendSystemManager
import de.ggnext.velocityCore.features.friends.commands.FriendCommand
import de.ggnext.velocityCore.features.hub.HubCommand
import de.ggnext.velocityCore.features.maintenance.commands.EditMtCommand
import de.ggnext.velocityCore.features.maintenance.commands.ToggleMtCommand
import de.ggnext.velocityCore.features.maintenance.listener.MaintenanceListener
import de.ggnext.velocityCore.features.party.PartySystemManager
import de.ggnext.velocityCore.features.party.commands.PartyCommand
import de.ggnext.velocityCore.features.players.PlayerManager
import de.ggnext.velocityCore.features.players.listener.PlayerListener
import de.ggnext.velocityCore.features.punishment.PunishmentChatListener
import de.ggnext.velocityCore.features.punishment.PunishmentManager
import de.ggnext.velocityCore.features.punishment.commands.BanCommands
import de.ggnext.velocityCore.features.punishment.commands.HistoryCommand
import de.ggnext.velocityCore.features.punishment.commands.MuteCommands
import de.ggnext.velocityCore.features.punishment.commands.WarnCommand
import de.ggnext.velocityCore.features.team.TeamChatListener
import de.ggnext.velocityCore.features.verify.VerifyCommand
import de.ggnext.velocityCore.features.verify.VerifyManager
import de.ggnext.velocityCore.utils.CommandRegistry
import de.ggnext.velocityCore.utils.CommandUtils
import java.nio.file.Path

@Plugin(
    id = "velocity-core",
    name = "Velocity-Core",
    version = "1.0-SNAPSHOT",
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
        private val config = configManager.config
        private lateinit var mongoManager: MongoManager
        private lateinit var playerManager: PlayerManager
        private lateinit var punishmentManager: PunishmentManager
        private lateinit var commandUtils: CommandUtils
        private lateinit var verifyManager: VerifyManager
        private lateinit var partySystemManager: PartySystemManager
        private lateinit var friendSystemManager: FriendSystemManager

        @Subscribe
        suspend fun onProxyInitialization(event: ProxyInitializeEvent) {
            configManager.load()
            configManager.save()
            if (configManager.config.prod) LogControl.setLevel(LogLevel.INFO) else LogControl.setLevel(LogLevel.DEBUG)

            SentryBuilder.init(
                SentryConfig(
                    config.sentryDSN,
                    config.prod,
                ),
            )

            mongoManager = MongoManager(config.mongoConnection, config.database)
            playerManager = PlayerManager(mongoManager.database)
            punishmentManager = PunishmentManager(mongoManager.database)
            commandUtils = CommandUtils(server, playerManager)
            verifyManager = VerifyManager(mongoManager.database)
            partySystemManager = PartySystemManager(playerManager)
            friendSystemManager = FriendSystemManager(playerManager)

            registerCommands()

            server.eventManager.register(this, TeamChatListener(server))
            server.eventManager.registerSuspend(
                this,
                PlayerListener(playerManager, punishmentManager, partySystemManager, config),
            )
            server.eventManager.registerSuspend(this, PunishmentChatListener(punishmentManager))
            server.eventManager.register(this, MaintenanceListener(configManager.config))

            ContentSystem(mongoManager.database, scope).also { it.init() }
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

            commandRegistry.registerCommand(PartyCommand(partySystemManager, commandUtils, server, scope).command)

            commandRegistry.registerCommand(FriendCommand(friendSystemManager, commandUtils, server, scope).command)
        }
    }
