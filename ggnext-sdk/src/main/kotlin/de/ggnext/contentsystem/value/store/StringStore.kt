package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.ContentSystem
import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.StringValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document
import kotlin.reflect.KProperty

class StringStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): String =
        runCatching {
            ValueCache.getTyped<StringValue>(key).value
        }.getOrElse {
            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "STRING")
                    .append("value", key)

            ContentSystem.instance.ensureDefault(doc)

            key
        }
}
