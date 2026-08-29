package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.MaterialValue
import kotlinx.coroutines.launch
import org.bson.Document
import org.bukkit.Material
import kotlin.reflect.KProperty

class MaterialStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Material =
        runCatching {
            ValueCache.getTyped<MaterialValue>(key).value
        }.getOrElse {
            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "MATERIAL")
                    .append("value", "STICK")

            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }

            Material.valueOf("STICK")
        }
}
