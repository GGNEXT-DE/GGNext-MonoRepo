package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.ContentSystem
import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.MaterialValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

            ContentSystem.instance.ensureDefault(doc)

            Material.valueOf("STICK")
        }
}
