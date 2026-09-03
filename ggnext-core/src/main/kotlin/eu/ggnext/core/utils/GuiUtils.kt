package eu.ggnext.core.utils

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun ItemStack.name(name: Component): ItemStack {
    itemMeta =
        itemMeta.also { meta ->
            meta.displayName(name)
        }
    return this
}

fun ItemStack.description(description: List<Component>): ItemStack {
    itemMeta =
        itemMeta.also { meta ->
            meta.lore(description)
        }
    return this
}

fun createFiller(material: Material = Material.BLACK_STAINED_GLASS_PANE): ItemStack = ItemStack(material).name(Component.text(""))

fun HumanEntity.toPlayer(): Player? = this as? Player
