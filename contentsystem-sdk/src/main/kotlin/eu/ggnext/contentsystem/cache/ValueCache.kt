package eu.ggnext.contentsystem.cache

import eu.ggnext.contentsystem.value.types.ConfigValue
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for all [eu.ggnext.contentsystem.value.types.ConfigValue] instances, keyed by their string identifier.
 * Automatically updated by the MongoDB change stream via [ContentSystemUpdater].
 */
internal object ValueCache {
    val cache = ConcurrentHashMap<String, ConfigValue<*>>()

    fun set(value: ConfigValue<*>) {
        cache[value.key] = value
    }

    /**
     * Retrieves a value from the cache and casts it to the expected type [T].
     * Throws an [IllegalStateException] if the key is missing or the type does not match.
     */
    inline fun <reified T : ConfigValue<*>> getTyped(key: String): T =
        cache[key] as? T ?: error("Value '$key' not found or wrong type (expected ${T::class.simpleName})")

    /**
     * Retrieves a list of values from the cache whose keys start with the given [prefix]
     * and filters them to ensure they match the expected type [T].
     */
    inline fun <reified T : ConfigValue<*>> getByPrefix(prefix: String): List<T> =
        cache.values
            .filterIsInstance<T>()
            .filter { it.key.startsWith(prefix) }

    fun remove(key: String) {
        cache.remove(key)
    }
}
