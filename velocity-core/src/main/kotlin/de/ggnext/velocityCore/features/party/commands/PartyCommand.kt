package de.ggnext.velocityCore.features.party.commands

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ProxyServer
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.features.party.AcceptResult
import de.ggnext.velocityCore.features.party.DenyResult
import de.ggnext.velocityCore.features.party.DisbandResult
import de.ggnext.velocityCore.features.party.InviteResult
import de.ggnext.velocityCore.features.party.KickResult
import de.ggnext.velocityCore.features.party.LeaveResult
import de.ggnext.velocityCore.features.party.PartyMember
import de.ggnext.velocityCore.features.party.PartySystemManager
import de.ggnext.velocityCore.utils.CommandUtils
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import de.ggnext.velocityCore.utils.playerArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class PartyCommand(
    private val partySystemManager: PartySystemManager,
    private val commandUtils: CommandUtils,
    private val proxy: ProxyServer,
    private val scope: CoroutineScope,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("party")
            .executes { ctx -> runList(ctx.source) }
            .then(
                BrigadierCommand
                    .literalArgumentBuilder("list")
                    .executes { ctx -> runList(ctx.source) },
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("invite")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runInvite(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("accept")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runAccept(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("deny")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runDeny(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("kick")
                    .then(
                        playerArgument(proxy).executes { ctx ->
                            runKick(ctx.source, ctx.getArgument("player", String::class.java))
                        },
                    ),
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("leave")
                    .executes { ctx -> runLeave(ctx.source) },
            ).then(
                BrigadierCommand
                    .literalArgumentBuilder("disband")
                    .executes { ctx -> runDisband(ctx.source) },
            ).build()

    private fun notifyMembers(
        members: List<PartyMember>,
        exclude: UUID,
        key: String,
        args: List<String>,
    ) {
        val translation by TranslationStore(key)
        members.forEach { member ->
            if (member.id == exclude) return@forEach
            proxy.getPlayer(member.id).getOrNull()?.let { online ->
                online.sendMessage(translation.get(online.language(), args))
            }
        }
    }

    private fun runList(source: CommandSource): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val members = partySystemManager.getPartyMembers(player.uniqueId)
            if (members.isEmpty()) {
                val empty by TranslationStore("translations.velocity.partysystem.list.empty")
                player.sendMessage(empty.get(player.language()))
                return@launch
            }

            val header by TranslationStore("translations.velocity.partysystem.list.header")
            player.sendMessage(header.get(player.language(), listOf(members.size.toString())))
            members.forEach { member ->
                val key =
                    if (member.isLeader) {
                        "translations.velocity.partysystem.list.leader"
                    } else {
                        "translations.velocity.partysystem.list.entry"
                    }
                val entry by TranslationStore(key)
                player.sendMessage(entry.get(player.language(), listOf(member.username)))
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runInvite(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val targetId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (partySystemManager.invitePlayer(player.uniqueId, targetId)) {
                InviteResult.SENT -> {
                    val sent by TranslationStore("translations.velocity.partysystem.invite.sent")
                    player.sendMessage(sent.get(player.language(), listOf(targetName)))

                    proxy.getPlayer(targetId).getOrNull()?.let { target ->
                        val received by TranslationStore("translations.velocity.partysystem.invite.received")
                        target.sendMessage(received.get(target.language(), listOf(player.username)))
                    }
                }

                InviteResult.NOT_LEADER -> {
                    val msg by TranslationStore("translations.velocity.partysystem.invite.not_leader")
                    player.sendMessage(msg.get(player.language()))
                }

                InviteResult.ALREADY_INVITED -> {
                    val msg by TranslationStore("translations.velocity.partysystem.invite.already_invited")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                InviteResult.TARGET_IN_PARTY -> {
                    val msg by TranslationStore("translations.velocity.partysystem.invite.target_in_party")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                InviteResult.PARTY_FULL -> {
                    val msg by TranslationStore("translations.velocity.partysystem.invite.full")
                    player.sendMessage(msg.get(player.language()))
                }

                InviteResult.SELF -> {
                    val msg by TranslationStore("translations.velocity.partysystem.invite.self")
                    player.sendMessage(msg.get(player.language()))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runAccept(
        source: CommandSource,
        leaderName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val leaderId = commandUtils.resolveTargetUUID(leaderName, player) ?: return@launch

            when (partySystemManager.acceptInvite(player.uniqueId, leaderId)) {
                AcceptResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.partysystem.accept.success")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))

                    val members = partySystemManager.getPartyMembers(player.uniqueId)
                    notifyMembers(
                        members,
                        exclude = player.uniqueId,
                        key = "translations.velocity.partysystem.accept.notify",
                        args = listOf(player.username),
                    )
                }

                AcceptResult.NO_INVITE -> {
                    val msg by TranslationStore("translations.velocity.partysystem.accept.no_invite")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))
                }

                AcceptResult.EXPIRED -> {
                    val msg by TranslationStore("translations.velocity.partysystem.accept.expired")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))
                }

                AcceptResult.PARTY_FULL -> {
                    val msg by TranslationStore("translations.velocity.partysystem.accept.full")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))
                }

                AcceptResult.ALREADY_IN_PARTY -> {
                    val msg by TranslationStore("translations.velocity.partysystem.accept.already_in_party")
                    player.sendMessage(msg.get(player.language()))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runDeny(
        source: CommandSource,
        leaderName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val leaderId = commandUtils.resolveTargetUUID(leaderName, player) ?: return@launch

            when (partySystemManager.denyInvite(player.uniqueId, leaderId)) {
                DenyResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.partysystem.deny.success")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))
                }

                DenyResult.NO_INVITE -> {
                    val msg by TranslationStore("translations.velocity.partysystem.deny.no_invite")
                    player.sendMessage(msg.get(player.language(), listOf(leaderName)))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runKick(
        source: CommandSource,
        targetName: String,
    ): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val targetId = commandUtils.resolveTargetUUID(targetName, player) ?: return@launch

            when (partySystemManager.kickPlayer(player.uniqueId, targetId)) {
                KickResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.partysystem.kick.success")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))

                    proxy.getPlayer(targetId).getOrNull()?.let { target ->
                        val notify by TranslationStore("translations.velocity.partysystem.kick.notify")
                        target.sendMessage(notify.get(target.language(), listOf(player.username)))
                    }
                }

                KickResult.NOT_LEADER -> {
                    val msg by TranslationStore("translations.velocity.partysystem.kick.not_leader")
                    player.sendMessage(msg.get(player.language()))
                }

                KickResult.NOT_MEMBER -> {
                    val msg by TranslationStore("translations.velocity.partysystem.kick.not_member")
                    player.sendMessage(msg.get(player.language(), listOf(targetName)))
                }

                KickResult.CANNOT_KICK_SELF -> {
                    val msg by TranslationStore("translations.velocity.partysystem.kick.cannot_kick_self")
                    player.sendMessage(msg.get(player.language()))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runLeave(source: CommandSource): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val before = partySystemManager.getPartyMembers(player.uniqueId)

            when (partySystemManager.leaveParty(player.uniqueId)) {
                LeaveResult.LEFT -> {
                    val msg by TranslationStore("translations.velocity.partysystem.leave.left")
                    player.sendMessage(msg.get(player.language()))

                    notifyMembers(
                        before,
                        exclude = player.uniqueId,
                        key = "translations.velocity.partysystem.leave.notify",
                        args = listOf(player.username),
                    )
                }

                LeaveResult.DISBANDED -> {
                    val msg by TranslationStore("translations.velocity.partysystem.leave.disbanded")
                    player.sendMessage(msg.get(player.language()))

                    notifyMembers(
                        before,
                        exclude = player.uniqueId,
                        key = "translations.velocity.partysystem.disband.notify",
                        args = listOf(player.username),
                    )
                }

                LeaveResult.NOT_IN_PARTY -> {
                    val msg by TranslationStore("translations.velocity.partysystem.leave.not_in_party")
                    player.sendMessage(msg.get(player.language()))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun runDisband(source: CommandSource): Int {
        val player = source.asPlayerOrNull() ?: return 1
        scope.launch {
            val before = partySystemManager.getPartyMembers(player.uniqueId)

            when (partySystemManager.disbandParty(player.uniqueId)) {
                DisbandResult.SUCCESS -> {
                    val msg by TranslationStore("translations.velocity.partysystem.disband.success")
                    player.sendMessage(msg.get(player.language()))

                    notifyMembers(
                        before,
                        exclude = player.uniqueId,
                        key = "translations.velocity.partysystem.disband.notify",
                        args = listOf(player.username),
                    )
                }

                DisbandResult.NOT_LEADER -> {
                    val msg by TranslationStore("translations.velocity.partysystem.disband.not_leader")
                    player.sendMessage(msg.get(player.language()))
                }
            }
        }
        return Command.SINGLE_SUCCESS
    }
}
