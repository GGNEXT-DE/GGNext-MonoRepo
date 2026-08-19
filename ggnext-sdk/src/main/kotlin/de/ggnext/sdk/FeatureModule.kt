package de.ggnext.sdk

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@JvmInline
value class FeatureId(
    val value: String,
)

class EventBinding(
    val channel: String,
    val decode: (Json, JsonElement) -> Any?,
)

interface FeatureModule<S : Any> {
    val id: FeatureId

    fun create(ctx: SdkContext): S

    fun events(): List<EventBinding> = emptyList()
}
