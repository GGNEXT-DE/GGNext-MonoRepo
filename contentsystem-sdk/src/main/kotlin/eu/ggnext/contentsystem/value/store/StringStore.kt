package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.StringValue
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

            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }

            key
        }
}
