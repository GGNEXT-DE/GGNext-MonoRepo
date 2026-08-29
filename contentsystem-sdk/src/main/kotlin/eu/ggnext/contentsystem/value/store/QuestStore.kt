package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.Quest
import eu.ggnext.contentsystem.value.types.QuestValue
import kotlin.reflect.KProperty

class QuestStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): List<Quest> = ValueCache.getByPrefix<QuestValue>(key).map { it.value }
}
