package eu.ggnext.dc.config

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class BotConfig(
    val TOKEN: String,
    val DATABASE_URI: String,
    val DATABASE_NAME: String,
    val GUILD_ID: String,
)
