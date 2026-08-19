package de.ggnext.backend

import de.ggnext.transport.RpcServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object BackendJson {
    val instance =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }
}

inline fun <reified Req, reified Res> RpcServer.on(
    route: String,
    json: Json,
    crossinline fn: suspend (Req) -> Res,
) {
    register(route) { payload ->
        val req = json.decodeFromJsonElement(serializer<Req>(), payload)
        json.encodeToJsonElement(serializer<Res>(), fn(req))
    }
}
