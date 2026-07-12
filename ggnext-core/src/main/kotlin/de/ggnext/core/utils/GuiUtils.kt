package de.ggnext.core.utils

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

public fun ItemStack.name(name: Component): ItemStack {
    itemMeta =
        itemMeta.also { meta ->
            meta.displayName(name)
        }
    return this
}

public fun ItemStack.description(description: List<Component>): ItemStack {
    itemMeta =
        itemMeta.also { meta ->
            meta.lore(description)
        }
    return this
}
