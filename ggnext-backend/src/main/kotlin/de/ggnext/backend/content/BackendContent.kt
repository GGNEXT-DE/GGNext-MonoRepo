package de.ggnext.backend.content

import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

class BackendContent {
    private val ints = ConcurrentHashMap<String, Int>()

    fun apply(doc: Document) {
        val key = doc.getString("_id") ?: return
        if (doc.getString("type") == "NUMBER") {
            (doc.get("value") as? Number)?.let { ints[key] = it.toInt() }
        }
    }

    fun remove(key: String) {
        ints.remove(key)
    }

    fun getInt(
        key: String,
        default: Int,
    ): Int = ints[key] ?: default
}
