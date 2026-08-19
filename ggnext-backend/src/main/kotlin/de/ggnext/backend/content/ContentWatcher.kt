package de.ggnext.backend.content

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.FullDocument
import com.mongodb.client.model.changestream.OperationType
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.ggnext.protocol.Channels
import de.ggnext.protocol.content.ContentEvent
import de.ggnext.transport.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.bson.Document

class ContentWatcher(
    mongoDatabase: MongoDatabase,
    private val eventBus: EventBus,
    private val json: Json,
    private val content: BackendContent,
    private val scope: CoroutineScope,
) {
    private val collection = mongoDatabase.getCollection<Document>("values")

    suspend fun preload() {
        collection.find().collect { content.apply(it) }
    }

    fun start(): Job =
        scope.launch(Dispatchers.IO) {
            collection
                .watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .collect { change ->
                    when (change.operationType) {
                        OperationType.INSERT, OperationType.UPDATE, OperationType.REPLACE -> {
                            val doc = change.fullDocument ?: return@collect
                            content.apply(doc)
                            publishChanged(doc)
                        }

                        OperationType.DELETE -> {
                            val key = change.documentKey?.getString("_id")?.value ?: return@collect
                            content.remove(key)
                            eventBus.publish(
                                Channels.EVENT_CONTENT,
                                json.encodeToJsonElement(ContentEvent.serializer(), ContentEvent.Deleted(key)),
                            )
                        }

                        else -> {
                            return@collect
                        }
                    }
                }
        }

    suspend fun snapshot(): List<String> = collection.find().map { it.toJson() }.toList()

    suspend fun ensureDefault(documentJson: String): Boolean {
        val doc = runCatching { Document.parse(documentJson) }.getOrNull() ?: return false
        val key = doc.getString("_id") ?: return false
        val existing = collection.find(Filters.eq("_id", key)).firstOrNull()
        if (existing != null) return false
        collection.insertOne(doc)
        content.apply(doc)
        publishChanged(doc)
        return true
    }

    private suspend fun publishChanged(doc: Document) {
        eventBus.publish(
            Channels.EVENT_CONTENT,
            json.encodeToJsonElement(ContentEvent.serializer(), ContentEvent.Changed(doc.toJson())),
        )
    }
}
