package eu.ggnext.railway.auction

import org.bukkit.inventory.ItemStack
import java.util.Base64

fun ItemStack.serializeForAuction(): String = Base64.getEncoder().encodeToString(serializeAsBytes())

fun String.deserializeAuctionItem(): ItemStack = ItemStack.deserializeBytes(Base64.getDecoder().decode(this))
