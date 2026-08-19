package de.ggnext.sdk

import de.ggnext.transport.EventBus
import de.ggnext.transport.RedisTransport
import de.ggnext.transport.RpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@DslMarker
annotation class GGNextDsl

@GGNextDsl
class RedisConfigBuilder {
    var host: String = "127.0.0.1"
    var port: Int = 6379
    var password: String? = null
    var url: String? = null
    var requestTimeout: Duration = 3.seconds

    internal fun uri(): String {
        url?.let { return it }
        val auth = password?.let { ":$it@" } ?: ""
        return "redis://$auth$host:$port"
    }
}

@GGNextDsl
class GGNextBuilder {
    private val redis = RedisConfigBuilder()
    private var instanceId: String = "ggnext"
    private val modules = mutableListOf<FeatureModule<*>>()

    fun redis(block: RedisConfigBuilder.() -> Unit) {
        redis.apply(block)
    }

    fun identity(serverId: String) {
        instanceId = serverId
    }

    fun install(module: FeatureModule<*>) {
        modules += module
    }

    internal fun build(): GGNext {
        val json = SdkJson.instance
        val transport = RedisTransport(redis.uri(), json)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val rpcClient = RpcClient(transport, instanceId).also { it.start() }
        val eventBus = EventBus(transport).also { it.start() }
        val dispatcher = EventDispatcher(eventBus, json)

        val ctx = SdkContext(rpcClient, dispatcher, json, instanceId, redis.requestTimeout, scope)

        val features = HashMap<FeatureId, Any>()
        for (module in modules) {
            features[module.id] = module.create(ctx)
            for (binding in module.events()) dispatcher.bind(binding.channel, binding.decode)
        }
        dispatcher.startChannels()

        return GGNext(ctx, features)
    }
}

fun ggnext(block: GGNextBuilder.() -> Unit): GGNext = GGNextBuilder().apply(block).build()
