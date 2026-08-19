package de.ggnext.transport

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.serialization.json.Json

class RedisTransport(
    redisUri: String,
    val json: Json = defaultJson,
) : AutoCloseable {
    private val client = RedisClient.create(redisUri)

    val commands: StatefulRedisConnection<String, String> = client.connect()

    fun newConnection(): StatefulRedisConnection<String, String> = client.connect()

    fun newPubSubConnection(): StatefulRedisPubSubConnection<String, String> = client.connectPubSub()

    override fun close() {
        commands.close()
        client.shutdown()
    }

    companion object {
        val defaultJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}
