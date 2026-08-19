package de.ggnext.contentsystem

import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.updater.ContentSystemUpdater
import de.ggnext.sdk.feature.ContentSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bson.Document

class ContentSystem(
    private val content: ContentSdk,
    val scope: CoroutineScope,
) {
    companion object {
        lateinit var instance: ContentSystem
            private set
    }

    suspend fun init() {
        instance = this
        content.snapshot().forEach { applyDoc(it) }
        content.onChanged { changed -> applyDoc(changed.documentJson) }
        content.onDeleted { deleted -> ValueCache.remove(deleted.key) }
    }

    fun ensureDefault(doc: Document) {
        scope.launch { content.ensureDefault(doc.toJson()) }
    }

    private fun applyDoc(documentJson: String) {
        val doc = runCatching { Document.parse(documentJson) }.getOrNull() ?: return
        ContentSystemUpdater.onUpdate(doc)
    }

    fun shutdown() {}
}
