package eu.ggnext.dc.config

import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

object ConfigManager {
    @OptIn(ExperimentalSerializationApi::class)
    fun loadConfig(): BotConfig {
        val dotenv = dotenv { ignoreIfMissing = true }
        val map = dotenv.entries().associate { it.key to it.value }
        return Properties.decodeFromStringMap(map)
    }
}
