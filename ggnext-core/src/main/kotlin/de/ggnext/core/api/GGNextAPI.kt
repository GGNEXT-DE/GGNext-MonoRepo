package de.ggnext.core.api

import de.ggnext.core.db.MongoManager
import de.ggnext.core.economy.EconomyService
import kotlinx.serialization.json.Json

object GGNextAPI {
    lateinit var mongoManager: MongoManager
    lateinit var economyService: EconomyService
    val json = Json { ignoreUnknownKeys = true }
}
