package de.ggnext.sdk

import de.ggnext.transport.EventBus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class EventDispatcher(
    private val eventBus: EventBus,
    private val json: Json,
) {
    private val listeners = CopyOnWriteArrayList<Pair<Class<*>, suspend (Any) -> Unit>>()
    private val bindings = ConcurrentHashMap<String, (Json, JsonElement) -> Any?>()
    private val started = ConcurrentHashMap.newKeySet<String>()

    fun bind(
        channel: String,
        decode: (Json, JsonElement) -> Any?,
    ) {
        bindings[channel] = decode
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> on(
        type: Class<T>,
        handler: suspend (T) -> Unit,
    ) {
        listeners.add(type to (handler as suspend (Any) -> Unit))
    }

    fun startChannels() {
        for ((channel, decode) in bindings) {
            if (started.add(channel)) {
                eventBus.subscribe(channel) { payload ->
                    val value = runCatching { decode(json, payload) }.getOrNull() ?: return@subscribe
                    dispatch(value)
                }
            }
        }
    }

    private suspend fun dispatch(value: Any) {
        for ((type, handler) in listeners) {
            if (type.isInstance(value)) handler(value)
        }
    }
}
