package eu.ggnext.contentsystem.value.types

import org.bukkit.Material

internal data class MaterialValue(
    override val key: String,
    override val value: Material,
) : ConfigValue<Material>
