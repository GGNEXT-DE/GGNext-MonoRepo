package eu.ggnext.contentsystem.value.types

/**
 * Base interface for all config value types stored in the [ValueCache].
 * Each implementation represents a specific data type (e.g. String, Number, Material).
 *
 * @param T the type of the wrapped value
 */
internal sealed interface ConfigValue<T> {
    val key: String
    val value: T
}
