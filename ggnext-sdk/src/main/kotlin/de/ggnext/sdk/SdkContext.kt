package de.ggnext.sdk

import de.ggnext.protocol.rpc.Status
import de.ggnext.transport.RpcClient
import de.ggnext.transport.RpcException
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration

object SdkJson {
    val instance =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }
}

class SdkContext(
    @PublishedApi internal val rpcClient: RpcClient,
    val events: EventDispatcher,
    @PublishedApi internal val json: Json,
    val instanceId: String,
    @PublishedApi internal val requestTimeout: Duration,
    val scope: CoroutineScope,
)

suspend inline fun <reified Req, reified Res> SdkContext.rpc(
    route: String,
    req: Req,
): Res {
    val payload = json.encodeToJsonElement(serializer<Req>(), req)
    val reply = rpcClient.call(route, payload, requestTimeout)
    if (reply.status == Status.ERROR) throw RpcException(reply.error ?: "rpc error: $route")
    return json.decodeFromJsonElement(serializer<Res>(), reply.payload)
}
