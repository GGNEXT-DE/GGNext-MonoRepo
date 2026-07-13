package de.ggnext.core.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

fun Player.language(): String =
    when (locale().language) {
        "de" -> "de"
        else -> "en"
    }

fun UUID.player(): Player? = Bukkit.getPlayer(this)
