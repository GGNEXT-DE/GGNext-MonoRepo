package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.Quest
import de.ggnext.contentsystem.value.types.QuestValue
import kotlin.reflect.KProperty

class QuestStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): List<Quest> = ValueCache.getByPrefix<QuestValue>(key).map { it.value }
}
