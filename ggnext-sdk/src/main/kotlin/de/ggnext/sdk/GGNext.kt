package de.ggnext.sdk

class GGNext(
    val ctx: SdkContext,
    private val features: Map<FeatureId, Any>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <S : Any> require(module: FeatureModule<S>): S =
        features[module.id] as? S
            ?: error("Feature '${module.id.value}' not installed — add install(${module.id.value}) to ggnext { }")

    inline fun <reified T : Any> on(noinline handler: suspend (T) -> Unit) = ctx.events.on(T::class.java, handler)
}
