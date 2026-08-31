package eu.ggnext.dc.config

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class BotConfig(
    val TOKEN: String,
    val DATABASE_URI: String,
    val DATABASE_NAME: String,
    val GUILD_ID: String,
    val TICKET_LOG_ID: String,
    val SUPPORT_ID: String,
    val MANAGER_ID: String,
    val VERIFIED_ID: String,
)
