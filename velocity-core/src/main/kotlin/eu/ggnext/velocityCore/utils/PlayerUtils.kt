package eu.ggnext.velocityCore.utils

import com.velocitypowered.api.proxy.Player
import java.util.Locale

fun Player.language(): String =
    when (this.effectiveLocale) {
        Locale.GERMAN, Locale.GERMANY -> "de"
        else -> "en"
    }
