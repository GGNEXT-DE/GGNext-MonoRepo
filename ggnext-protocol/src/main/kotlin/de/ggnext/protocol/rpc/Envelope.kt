package de.ggnext.protocol.rpc

import de.ggnext.protocol.PROTOCOL_VERSION
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class Status {
    OK,
    ERROR,
}

@Serializable
data class RpcEnvelope(
    val id: String,
    val route: String,
    val payload: JsonElement,
    val v: Int = PROTOCOL_VERSION,
    val replyTo: String? = null,
    val status: Status = Status.OK,
    val error: String? = null,
)

@Serializable
data class EventEnvelope(
    val channel: String,
    val payload: JsonElement,
    val v: Int = PROTOCOL_VERSION,
)
