package eu.ggnext.contentsystem

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import eu.ggnext.contentsystem.updater.MongoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class ContentSystem(
    private val database: MongoDatabase,
    val scope: CoroutineScope,
) {
    companion object {
        lateinit var instance: ContentSystem
            private set
    }

    internal lateinit var mongoManager: MongoManager
    private var watchJob: Job? = null

    suspend fun init() {
        instance = this
        mongoManager = MongoManager(database, scope)
        mongoManager.preload()
        watchJob = mongoManager.start()
    }

    fun shutdown() {
        watchJob?.cancel()
    }
}
