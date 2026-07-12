package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.ContentSystem
import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.MaterialValue
import de.ggnext.contentsystem.value.types.NumberValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document
import org.bukkit.Material
import kotlin.reflect.KProperty

class NumberStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Double =
        runCatching {
            ValueCache.getTyped<NumberValue>(key).value
        }.getOrElse {
            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "NUMBER")
                    .append("value", 1.0)

            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }

            1.0
        }
}
