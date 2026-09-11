package eu.ggnext.core.api

import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.economy.EconomyService
import eu.ggnext.common.job.JobManager
import eu.ggnext.core.input.PlayerInputManager
import eu.ggnext.core.scoreboard.ScoreBoardManager
import kotlinx.serialization.json.Json

object GGNextAPI {
    lateinit var mongoManager: MongoManager
    lateinit var economyService: EconomyService
    lateinit var scoreBoardManager: ScoreBoardManager
    lateinit var playerInputManager: PlayerInputManager
    lateinit var jobManager: JobManager
    val json = Json { ignoreUnknownKeys = true }
}
