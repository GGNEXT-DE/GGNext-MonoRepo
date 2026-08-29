package eu.ggnext.contentsystem.updater

import com.mongodb.client.model.changestream.FullDocument
import com.mongodb.client.model.changestream.OperationType
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import eu.ggnext.contentsystem.cache.ValueCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document

/**
 * Manages the MongoDB connection, preloads all values into the [ValueCache],
 * and watches for real-time changes via a change stream.
 *
 * @param mongoDatabase the database to connect to
 * @param scope the coroutine scope used for the change stream listener
 */
internal class MongoManager(
    private val mongoDatabase: MongoDatabase,
    private val scope: CoroutineScope,
) {
    val collection = mongoDatabase.getCollection<Document>("values")

    /**
     * Loads all existing values from the collection into the [ValueCache].
     * Must be called before [start].
     */
    suspend fun preload() {
        collection.find().collect { doc ->
            ContentSystemUpdater.onUpdate(doc)
        }
    }

    /**
     * Starts listening to the MongoDB change stream and keeps the [ValueCache] in sync.
     * Returns a [Job] that can be cancelled to stop the listener.
     */
    fun start() =
        scope.launch(Dispatchers.IO) {
            collection
                .watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .collect { change ->
                    val doc = change.fullDocument ?: return@collect
                    when (change.operationType) {
                        OperationType.INSERT, OperationType.UPDATE, OperationType.REPLACE -> {
                            ContentSystemUpdater.onUpdate(doc)
                        }

                        OperationType.DELETE -> {
                            val key = change.documentKey?.getString("_id") ?: return@collect
                            ValueCache.remove(key.toString())
                        }

                        else -> {
                            return@collect
                        }
                    }
                }
        }
}
