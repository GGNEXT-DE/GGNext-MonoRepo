package de.ggnext.transport

import de.ggnext.protocol.rpc.EventEnvelope
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class EventBus(
    private val transport: RedisTransport,
) {
    private val json = transport.json
    private val commands = transport.commands.coroutines()
    private val handlers = ConcurrentHashMap<String, CopyOnWriteArrayList<suspend (JsonElement) -> Unit>>()
    private val subScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pubSub = transport.newPubSubConnection()
    private val subscribed = ConcurrentHashMap.newKeySet<String>()

    fun start() {
        pubSub.addListener(
            object : RedisPubSubAdapter<String, String>() {
                override fun message(
                    channel: String,
                    message: String,
                ) {
                    val list = handlers[channel] ?: return
                    val env = runCatching { json.decodeFromString(EventEnvelope.serializer(), message) }.getOrNull() ?: return
                    for (h in list) subScope.launch { runCatching { h(env.payload) } }
                }
            },
        )
    }

    suspend fun publish(
        channel: String,
        payload: JsonElement,
    ) {
        val env = EventEnvelope(channel, payload)
        commands.publish(channel, json.encodeToString(EventEnvelope.serializer(), env))
    }

    fun subscribe(
        channel: String,
        handler: suspend (JsonElement) -> Unit,
    ) {
        handlers.computeIfAbsent(channel) { CopyOnWriteArrayList() }.add(handler)
        if (subscribed.add(channel)) {
            pubSub.sync().subscribe(channel)
        }
    }
}
