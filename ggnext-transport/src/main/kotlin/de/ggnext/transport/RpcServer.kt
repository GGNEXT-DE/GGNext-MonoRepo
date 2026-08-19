package de.ggnext.transport

import de.ggnext.protocol.Channels
import de.ggnext.protocol.rpc.RpcEnvelope
import de.ggnext.protocol.rpc.Status
import io.lettuce.core.Consumer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RpcServer(
    private val transport: RedisTransport,
    private val scope: CoroutineScope,
    private val workers: Int = 4,
) {
    private val json = transport.json
    private val stream = Channels.RPC_REQUEST_STREAM
    private val group = Channels.RPC_CONSUMER_GROUP
    private val handlers = ConcurrentHashMap<String, suspend (JsonElement) -> JsonElement>()
    private val publish = transport.commands.coroutines()

    fun register(
        route: String,
        handler: suspend (JsonElement) -> JsonElement,
    ) {
        handlers[route] = handler
    }

    suspend fun start() {
        runCatching {
            transport.commands.coroutines().xgroupCreate(
                XReadArgs.StreamOffset.from(stream, "0"),
                group,
                XGroupCreateArgs.Builder.mkstream(true),
            )
        }
        repeat(workers) { w -> scope.launch { worker("worker-$w") } }
    }

    private suspend fun worker(consumer: String) {
        val conn = transport.newConnection()
        val cmd = conn.coroutines()
        while (scope.isActive) {
            val messages =
                runCatching {
                    cmd
                        .xreadgroup(
                            Consumer.from(group, consumer),
                            XReadArgs.Builder.block(2000).count(16),
                            XReadArgs.StreamOffset.lastConsumed(stream),
                        ).toList()
                }.getOrElse { emptyList() }
            for (msg in messages) {
                val body = msg.body["e"]
                if (body != null) handle(body)
                cmd.xack(stream, group, msg.id)
            }
        }
    }

    private suspend fun handle(body: String) {
        val request = runCatching { json.decodeFromString(RpcEnvelope.serializer(), body) }.getOrNull() ?: return
        val reply =
            try {
                val handler = handlers[request.route] ?: throw RpcException("no handler for ${request.route}")
                val out = handler(request.payload)
                request.copy(payload = out, replyTo = null, status = Status.OK, error = null)
            } catch (e: Exception) {
                request.copy(replyTo = null, status = Status.ERROR, error = e.message ?: e::class.simpleName)
            }
        val target = request.replyTo ?: return
        publish.publish(target, json.encodeToString(RpcEnvelope.serializer(), reply))
    }
}
