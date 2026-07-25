package de.ggnext.core.api

import de.ggnext.common.db.MongoManager
import de.ggnext.common.job.JobManager
import de.ggnext.core.economy.EconomyService
import de.ggnext.core.scoreboard.ScoreBoardManager
import kotlinx.serialization.json.Json

object GGNextAPI {
    lateinit var mongoManager: MongoManager
    lateinit var economyService: EconomyService
    lateinit var scoreBoardManager: ScoreBoardManager
    lateinit var jobManager: JobManager
    val json = Json { ignoreUnknownKeys = true }
}
