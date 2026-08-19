package de.ggnext.contentsystem.value.types

internal data class NumberValue(
    override val key: String,
    override val value: Int,
) : ConfigValue<Int>
