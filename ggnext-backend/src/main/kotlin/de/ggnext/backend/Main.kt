package de.ggnext.backend

import de.ggnext.backend.config.BackendConfig
import de.ggnext.backend.content.BackendContent
import de.ggnext.backend.content.ContentWatcher
import de.ggnext.backend.friend.FriendSystemManager
import de.ggnext.backend.party.PartyManager
import de.ggnext.backend.player.PlayerManager
import de.ggnext.backend.presence.PresenceManager
import de.ggnext.backend.punishment.PunishmentManager
import de.ggnext.backend.verify.VerifyManager
import de.ggnext.common.db.MongoManager
import de.ggnext.common.economy.EconomyService
import de.ggnext.common.sentry.SentryBuilder
import de.ggnext.common.sentry.SentryConfig
import de.ggnext.protocol.Channels
import de.ggnext.protocol.Routes
import de.ggnext.protocol.content.ContentEnsureDefaultRequest
import de.ggnext.protocol.content.ContentEnsureDefaultResponse
import de.ggnext.protocol.content.ContentSnapshotRequest
import de.ggnext.protocol.content.ContentSnapshotResponse
import de.ggnext.protocol.economy.EconomyChangeRequest
import de.ggnext.protocol.economy.EconomyGetRequest
import de.ggnext.protocol.friend.AcceptResult
import de.ggnext.protocol.friend.DenyResult
import de.ggnext.protocol.friend.FriendActionRequest
import de.ggnext.protocol.friend.FriendAddRequest
import de.ggnext.protocol.friend.FriendEvent
import de.ggnext.protocol.friend.FriendListRequest
import de.ggnext.protocol.friend.FriendListResponse
import de.ggnext.protocol.friend.FriendRequestResult
import de.ggnext.protocol.friend.RemoveResult
import de.ggnext.protocol.party.PartyActorRequest
import de.ggnext.protocol.party.PartyInviteActionRequest
import de.ggnext.protocol.party.PartyInviteRequest
import de.ggnext.protocol.party.PartyKickRequest
import de.ggnext.protocol.party.PartyMembersResponse
import de.ggnext.protocol.player.Player
import de.ggnext.protocol.player.PlayerActorRequest
import de.ggnext.protocol.player.PlayerGetByNameRequest
import de.ggnext.protocol.player.PlayerGetRequest
import de.ggnext.protocol.player.PlayerLoginRequest
import de.ggnext.protocol.presence.OnlineQuery
import de.ggnext.protocol.presence.OnlineResponse
import de.ggnext.protocol.presence.PresenceUpdateRequest
import de.ggnext.protocol.punishment.PunishmentActionRequest
import de.ggnext.protocol.punishment.PunishmentActiveRequest
import de.ggnext.protocol.punishment.PunishmentEntry
import de.ggnext.protocol.punishment.PunishmentHistoryRequest
import de.ggnext.protocol.punishment.PunishmentHistoryResponse
import de.ggnext.protocol.punishment.PunishmentRevokeRequest
import de.ggnext.protocol.punishment.PunishmentTempRequest
import de.ggnext.protocol.verify.VerifyCompleteRequest
import de.ggnext.protocol.verify.VerifyCreateRequest
import de.ggnext.protocol.verify.VerifyCreateResponse
import de.ggnext.protocol.verify.VerifyGetRequest
import de.ggnext.protocol.verify.VerifyResult
import de.ggnext.transport.EventBus
import de.ggnext.transport.RedisTransport
import de.ggnext.transport.RpcServer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.SetArgs
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun main(): Unit =
    runBlocking {
        val config = BackendConfig.load()
        SentryBuilder.init(SentryConfig(config.sentryDsn.ifBlank { null }, config.prod))

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val json = BackendJson.instance

        val mongo = MongoManager(config.mongo.connection, config.mongo.database)
        val transport = RedisTransport(config.redis.uri, json)
        val eventBus = EventBus(transport).also { it.start() }
        val rpcServer = RpcServer(transport, scope)

        val content = BackendContent()
        val playerManager = PlayerManager(mongo.database)
        val friendManager = FriendSystemManager(playerManager, content)
        val partyManager = PartyManager(playerManager, eventBus, json, content, transport)
        val verifyManager = VerifyManager(mongo.database)
        val presenceManager = PresenceManager(transport, eventBus, json)
        val contentWatcher = ContentWatcher(mongo.database, eventBus, json, content, scope)
        val economyService = EconomyService(mongo.database)
        val punishmentManager = PunishmentManager(mongo.database)

        registerFriendHandlers(rpcServer, json, friendManager, playerManager, eventBus)
        registerPartyHandlers(rpcServer, json, partyManager)
        registerVerifyHandlers(rpcServer, json, verifyManager)
        registerPresenceHandlers(rpcServer, json, presenceManager)
        registerContentHandlers(rpcServer, json, contentWatcher)
        registerEconomyHandlers(rpcServer, json, economyService)
        registerPlayerHandlers(rpcServer, json, playerManager)
        registerPunishmentHandlers(rpcServer, json, punishmentManager)

        contentWatcher.preload()
        partyManager.load()
        contentWatcher.start()
        rpcServer.start()

        val heartbeat = transport.commands.coroutines()
        scope.launch {
            while (true) {
                heartbeat.set("ggnext:backend:alive", System.currentTimeMillis().toString(), SetArgs.Builder.ex(10))
                delay(5000)
            }
        }

        println("[ggnext-backend] started — mongo=${config.mongo.database} redis=${config.redis.uri}")
        awaitCancellation()
    }

private fun registerFriendHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    friends: FriendSystemManager,
    players: PlayerManager,
    eventBus: EventBus,
) {
    server.on<FriendAddRequest, FriendRequestResult>(Routes.FRIEND_ADD, json) { req ->
        val result = friends.createRequest(req.actor, req.target)
        if (result == FriendRequestResult.SENT) {
            players.getPlayer(req.actor)?.let { actor ->
                eventBus.publish(
                    Channels.EVENT_FRIEND,
                    json.encodeToJsonElement(
                        FriendEvent.serializer(),
                        FriendEvent.RequestReceived(req.target, req.actor, actor.username),
                    ),
                )
            }
        }
        result
    }
    server.on<FriendActionRequest, AcceptResult>(Routes.FRIEND_ACCEPT, json) { req ->
        val result = friends.acceptRequest(req.actor, req.other)
        if (result == AcceptResult.SUCCESS) {
            players.getPlayer(req.actor)?.let { actor ->
                eventBus.publish(
                    Channels.EVENT_FRIEND,
                    json.encodeToJsonElement(
                        FriendEvent.serializer(),
                        FriendEvent.RequestAccepted(req.other, req.actor, actor.username),
                    ),
                )
            }
        }
        result
    }
    server.on<FriendActionRequest, DenyResult>(Routes.FRIEND_DENY, json) { friends.denyRequest(it.actor, it.other) }
    server.on<FriendActionRequest, RemoveResult>(Routes.FRIEND_REMOVE, json) { friends.removeFriend(it.actor, it.other) }
    server.on<FriendListRequest, FriendListResponse>(Routes.FRIEND_LIST, json) { FriendListResponse(friends.getFriends(it.actor)) }
}

private fun registerPartyHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    party: PartyManager,
) {
    server.on<PartyInviteRequest, de.ggnext.protocol.party.InviteResult>(Routes.PARTY_INVITE, json) {
        party.invitePlayer(it.inviter, it.target)
    }
    server.on<PartyInviteActionRequest, de.ggnext.protocol.party.AcceptResult>(Routes.PARTY_ACCEPT, json) {
        party.acceptInvite(it.actor, it.leader)
    }
    server.on<PartyInviteActionRequest, de.ggnext.protocol.party.DenyResult>(Routes.PARTY_DENY, json) {
        party.denyInvite(it.actor, it.leader)
    }
    server.on<PartyKickRequest, de.ggnext.protocol.party.KickResult>(Routes.PARTY_KICK, json) {
        party.kickPlayer(it.leader, it.target)
    }
    server.on<PartyActorRequest, de.ggnext.protocol.party.LeaveResult>(Routes.PARTY_LEAVE, json) {
        party.leaveParty(it.actor)
    }
    server.on<PartyActorRequest, de.ggnext.protocol.party.DisbandResult>(Routes.PARTY_DISBAND, json) {
        party.disbandParty(it.actor)
    }
    server.on<PartyActorRequest, PartyMembersResponse>(Routes.PARTY_MEMBERS, json) {
        PartyMembersResponse(party.getPartyMembers(it.actor))
    }
}

private fun registerVerifyHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    verify: VerifyManager,
) {
    server.on<VerifyCreateRequest, VerifyCreateResponse>(Routes.VERIFY_CREATE, json) {
        val (code, expiresAt) = verify.createVerification(it.playerId)
        VerifyCreateResponse(code, expiresAt)
    }
    server.on<VerifyCompleteRequest, VerifyResult>(Routes.VERIFY_COMPLETE, json) {
        verify.complete(it.code, it.discordId)
    }
    server.on<VerifyGetRequest, VerifyCreateResponse?>(Routes.VERIFY_GET, json) {
        verify.getActive(it.playerId)?.let { v -> VerifyCreateResponse(v.verifyCode, v.expiresAt) }
    }
}

private fun registerPresenceHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    presence: PresenceManager,
) {
    server.on<PresenceUpdateRequest, Boolean>(Routes.PRESENCE_UPDATE, json) { presence.update(it) }
    server.on<OnlineQuery, OnlineResponse>(Routes.PRESENCE_ONLINE, json) { OnlineResponse(presence.online(it.playerIds)) }
}

private fun registerContentHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    watcher: ContentWatcher,
) {
    server.on<ContentSnapshotRequest, ContentSnapshotResponse>(Routes.CONTENT_SNAPSHOT, json) {
        ContentSnapshotResponse(watcher.snapshot())
    }
    server.on<ContentEnsureDefaultRequest, ContentEnsureDefaultResponse>(Routes.CONTENT_ENSURE_DEFAULT, json) {
        ContentEnsureDefaultResponse(watcher.ensureDefault(it.documentJson))
    }
}

private fun registerEconomyHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    economy: EconomyService,
) {
    server.on<EconomyGetRequest, Int>(Routes.ECONOMY_GET, json) { economy.getGems(it.playerId) }
    server.on<EconomyChangeRequest, Boolean>(Routes.ECONOMY_ADD, json) { economy.addGems(it.playerId, it.amount) }
    server.on<EconomyChangeRequest, Boolean>(Routes.ECONOMY_REMOVE, json) { economy.removeGems(it.playerId, it.amount) }
}

private fun registerPlayerHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    players: PlayerManager,
) {
    server.on<PlayerGetRequest, Player?>(Routes.PLAYER_GET, json) { players.getPlayer(it.playerId) }
    server.on<PlayerGetByNameRequest, Player?>(Routes.PLAYER_GET_BY_NAME, json) { players.getPlayer(it.username) }
    server.on<PlayerLoginRequest, Player>(Routes.PLAYER_LOGIN, json) { players.login(it.playerId, it.username) }
    server.on<PlayerActorRequest, Boolean>(Routes.PLAYER_SAVE_PLAYTIME, json) {
        players.savePlaytime(it.playerId)
        true
    }
}

private fun registerPunishmentHandlers(
    server: RpcServer,
    json: kotlinx.serialization.json.Json,
    punishment: PunishmentManager,
) {
    server.on<PunishmentActionRequest, PunishmentEntry>(Routes.PUNISH_BAN, json) {
        punishment.ban(it.player, it.issuedBy, it.reason).toEntry()
    }
    server.on<PunishmentTempRequest, PunishmentEntry>(Routes.PUNISH_TEMP_BAN, json) {
        punishment.tempBan(it.player, it.issuedBy, it.durationMillis, it.reason).toEntry()
    }
    server.on<PunishmentActionRequest, PunishmentEntry>(Routes.PUNISH_MUTE, json) {
        punishment.mute(it.player, it.issuedBy, it.reason).toEntry()
    }
    server.on<PunishmentTempRequest, PunishmentEntry>(Routes.PUNISH_TEMP_MUTE, json) {
        punishment.tempMute(it.player, it.issuedBy, it.durationMillis, it.reason).toEntry()
    }
    server.on<PunishmentActionRequest, PunishmentEntry>(Routes.PUNISH_WARN, json) {
        punishment.warn(it.player, it.issuedBy, it.reason).toEntry()
    }
    server.on<PunishmentRevokeRequest, Boolean>(Routes.PUNISH_UNBAN, json) { punishment.revokeBan(it.player, it.revokedBy) }
    server.on<PunishmentRevokeRequest, Boolean>(Routes.PUNISH_UNMUTE, json) { punishment.revokeMute(it.player, it.revokedBy) }
    server.on<PunishmentHistoryRequest, PunishmentHistoryResponse>(Routes.PUNISH_HISTORY, json) {
        PunishmentHistoryResponse(punishment.getHistory(it.player, it.filter).map { p -> p.toEntry() })
    }
    server.on<PunishmentActiveRequest, PunishmentEntry?>(Routes.PUNISH_ACTIVE_BAN, json) {
        punishment.getActiveBan(it.player)?.toEntry()
    }
    server.on<PunishmentActiveRequest, PunishmentEntry?>(Routes.PUNISH_ACTIVE_MUTE, json) {
        punishment.getActiveMute(it.player)?.toEntry()
    }
}
