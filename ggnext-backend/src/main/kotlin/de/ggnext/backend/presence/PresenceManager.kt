package de.ggnext.backend.presence

import de.ggnext.protocol.Channels
import de.ggnext.protocol.presence.PresenceChanged
import de.ggnext.protocol.presence.PresenceUpdateRequest
import de.ggnext.transport.EventBus
import de.ggnext.transport.RedisTransport
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines
import kotlinx.serialization.json.Json
import java.util.UUID

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PresenceManager(
    transport: RedisTransport,
    private val eventBus: EventBus,
    private val json: Json,
) {
    private val onlineKey = "ggnext:online"
    private val commands = transport.commands.coroutines()

    suspend fun update(request: PresenceUpdateRequest): Boolean {
        val id = request.playerId.toString()
        if (request.online) {
            commands.sadd(onlineKey, id)
            commands.hset(
                "ggnext:presence:$id",
                mapOf(
                    "username" to request.username,
                    "server" to request.server,
                    "since" to System.currentTimeMillis().toString(),
                ),
            )
            commands.expire("ggnext:presence:$id", 60L)
        } else {
            commands.srem(onlineKey, id)
            commands.del("ggnext:presence:$id")
        }
        eventBus.publish(
            Channels.EVENT_PRESENCE,
            json.encodeToJsonElement(
                PresenceChanged.serializer(),
                PresenceChanged(request.playerId, request.username, request.server, request.online),
            ),
        )
        return true
    }

    suspend fun online(playerIds: List<UUID>): List<UUID> = playerIds.filter { commands.sismember(onlineKey, it.toString()) == true }
}
