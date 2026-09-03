package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.Translation
import eu.ggnext.contentsystem.value.types.TranslationValue
import kotlinx.coroutines.launch
import org.bson.Document
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

            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }

            defaultTranslation
        }
}
