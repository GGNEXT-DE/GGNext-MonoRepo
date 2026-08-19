package de.ggnext.transport

import de.ggnext.protocol.Channels
import de.ggnext.protocol.rpc.RpcEnvelope
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.XAddArgs
import io.lettuce.core.api.coroutines
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RpcClient(
    private val transport: RedisTransport,
    private val instanceId: String,
    private val streamMaxLen: Long = 10_000,
) {
    private val json = transport.json
    private val replyChannel = Channels.replyChannel(instanceId)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<RpcEnvelope>>()
    private val pubSub = transport.newPubSubConnection()
    private val commands = transport.commands.coroutines()

    fun start() {
        pubSub.addListener(
            object : RedisPubSubAdapter<String, String>() {
                override fun message(
                    channel: String,
                    message: String,
                ) {
                    if (channel != replyChannel) return
                    val env = runCatching { json.decodeFromString(RpcEnvelope.serializer(), message) }.getOrNull() ?: return
                    pending.remove(env.id)?.complete(env)
                }
            },
        )
        pubSub.sync().subscribe(replyChannel)
    }

    suspend fun call(
        route: String,
        payload: JsonElement,
        timeout: Duration,
    ): RpcEnvelope {
        val id = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<RpcEnvelope>()
        pending[id] = deferred
        val env = RpcEnvelope(id = id, route = route, payload = payload, replyTo = replyChannel)
        val body = json.encodeToString(RpcEnvelope.serializer(), env)
        commands.xadd(
            Channels.RPC_REQUEST_STREAM,
            XAddArgs.Builder.maxlen(streamMaxLen).approximateTrimming(),
            mapOf("e" to body),
        )
        return try {
            withTimeout(timeout) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            pending.remove(id)
            throw RpcTimeoutException(route)
        }
    }

    fun close() {
        pubSub.close()
    }
}
