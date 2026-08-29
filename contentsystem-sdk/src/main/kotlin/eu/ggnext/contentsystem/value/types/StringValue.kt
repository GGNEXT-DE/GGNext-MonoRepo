package eu.ggnext.contentsystem.value.types

internal data class StringValue(
    override val key: String,
    override val value: String,
) : ConfigValue<String>
