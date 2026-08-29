package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.NumberValue
import kotlinx.coroutines.launch
import org.bson.Document
import kotlin.reflect.KProperty

class NumberStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Int =
        runCatching {
            ValueCache.getTyped<NumberValue>(key).value
        }.getOrElse {
            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "NUMBER")
                    .append("value", 1)

            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }

            1
        }
}
