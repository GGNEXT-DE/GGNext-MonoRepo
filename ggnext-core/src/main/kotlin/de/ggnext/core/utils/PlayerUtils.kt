package de.ggnext.core.utils

import org.bukkit.entity.Player

fun Player.language(): String =
    when (locale().language) {
        "de" -> "de"
        else -> "en"
    }
