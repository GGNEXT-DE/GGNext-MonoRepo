package eu.ggnext.contentsystem.updater

import eu.ggnext.contentsystem.cache.ValueCache
import org.bson.Document

/**
 * Handles incoming updates from the MongoDB change stream
 * and reflects them in the [ValueCache].
 */
internal object ContentSystemUpdater {
    fun onUpdate(doc: Document) {
        val value = ConfigValueMapper.fromDoc(doc) ?: return
        ValueCache.set(value)
    }
}
