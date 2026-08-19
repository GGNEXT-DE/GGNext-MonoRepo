package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.ContentSystem
import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.Translation
import de.ggnext.contentsystem.value.types.TranslationValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document
import org.bukkit.Material
import kotlin.reflect.KProperty

class TranslationStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Translation =
        runCatching {
            ValueCache.getTyped<TranslationValue>(key).value
        }.getOrElse {
            val defaultTranslation = Translation(de = key, en = key)

            val valueDoc =
                Document()
                    .append("de", defaultTranslation.de)
                    .append("en", defaultTranslation.en)

            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "TRANSLATION")
                    .append("value", valueDoc)

            ContentSystem.instance.ensureDefault(doc)

            defaultTranslation
        }
}
