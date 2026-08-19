package de.ggnext.velocityCore

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
import de.ggnext.common.logging.LogControl
import de.ggnext.common.logging.LogLevel
import de.ggnext.common.sentry.SentryBuilder
import de.ggnext.common.sentry.SentryConfig
import de.ggnext.contentsystem.ContentSystem
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.feature.Content
import de.ggnext.sdk.feature.Economy
import de.ggnext.sdk.feature.Friends
import de.ggnext.sdk.feature.Party
import de.ggnext.sdk.feature.Players
import de.ggnext.sdk.feature.Presence
import de.ggnext.sdk.feature.Punishment
import de.ggnext.sdk.feature.Verify
import de.ggnext.sdk.feature.content
import de.ggnext.sdk.feature.economy
import de.ggnext.sdk.feature.friends
import de.ggnext.sdk.feature.party
import de.ggnext.sdk.feature.players
import de.ggnext.sdk.feature.punishment
import de.ggnext.sdk.feature.verify
import de.ggnext.sdk.ggnext
import de.ggnext.velocityCore.config.ConfigManager
import de.ggnext.velocityCore.config.VelocityConfig
import de.ggnext.velocityCore.features.friends.commands.FriendCommand
import de.ggnext.velocityCore.features.hub.HubCommand
import de.ggnext.velocityCore.features.maintenance.commands.EditMtCommand
import de.ggnext.velocityCore.features.maintenance.commands.ToggleMtCommand
import de.ggnext.velocityCore.features.maintenance.listener.MaintenanceListener
import de.ggnext.velocityCore.features.party.commands.PartyCommand
import de.ggnext.velocityCore.features.players.listener.PlayerListener
import de.ggnext.velocityCore.features.punishment.PunishmentChatListener
import de.ggnext.velocityCore.features.punishment.commands.BanCommands
import de.ggnext.velocityCore.features.punishment.commands.HistoryCommand
import de.ggnext.velocityCore.features.punishment.commands.MuteCommands
import de.ggnext.velocityCore.features.punishment.commands.WarnCommand
import de.ggnext.velocityCore.features.team.TeamChatListener
import de.ggnext.velocityCore.features.verify.VerifyCommand
import de.ggnext.velocityCore.features.vote.VoteCommand
import de.ggnext.velocityCore.features.vote.VoteRewardListener
import de.ggnext.velocityCore.utils.CommandRegistry
import de.ggnext.velocityCore.utils.CommandUtils
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
        private lateinit var ggnext: GGNext
        private lateinit var commandUtils: CommandUtils

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

            ggnext =
                ggnext {
                    redis { url = config.redis.uri }
                    identity(config.serverId)
                    install(Players)
                    install(Friends)
                    install(Party)
                    install(Verify)
                    install(Presence)
                    install(Punishment)
                    install(Economy)
                    install(Content)
                }

            commandUtils = CommandUtils(server, ggnext.players)

            ContentSystem(ggnext.content, scope).also { it.init() }

            registerCommands()

            server.eventManager.register(this, TeamChatListener(server))
            server.eventManager.registerSuspend(
                this,
                PlayerListener(ggnext.players, ggnext.punishment, ggnext.party, config),
            )
            server.eventManager.registerSuspend(this, PunishmentChatListener(ggnext.punishment))
            server.eventManager.register(this, MaintenanceListener(configManager.config))
            server.eventManager.registerSuspend(this, VoteRewardListener(server, ggnext.players, ggnext.economy))
        }

        private fun registerCommands() {
            val commandRegistry = CommandRegistry(server.commandManager, this)

            commandRegistry.registerCommand(HubCommand(server).command)
            commandRegistry.registerCommand(ToggleMtCommand(configManager).command)
            commandRegistry.registerCommand(EditMtCommand(configManager).command)

            val banCommands = BanCommands(commandUtils, ggnext.punishment, scope, server)
            commandRegistry.registerCommand(banCommands.ban)
            commandRegistry.registerCommand(banCommands.unBan)
            commandRegistry.registerCommand(banCommands.tempBan)

            commandRegistry.registerCommand(HistoryCommand(commandUtils, ggnext.punishment, server, scope).history)

            val muteCommands = MuteCommands(server, commandUtils, ggnext.punishment, scope)
            commandRegistry.registerCommand(muteCommands.mute)
            commandRegistry.registerCommand(muteCommands.unMute)
            commandRegistry.registerCommand(muteCommands.tempMute)

            commandRegistry.registerCommand(WarnCommand(server, commandUtils, ggnext.punishment, scope).warn)

            commandRegistry.registerCommand(VerifyCommand(ggnext.verify, ggnext.players, scope).command)

            commandRegistry.registerCommand(VoteCommand().command)

            commandRegistry.registerCommand(PartyCommand(ggnext.party, commandUtils, server, scope).command)

            commandRegistry.registerCommand(FriendCommand(ggnext.friends, commandUtils, server, scope).command)
        }
    }
