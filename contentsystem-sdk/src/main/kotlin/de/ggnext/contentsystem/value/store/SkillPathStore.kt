package de.ggnext.contentsystem.value.store

import de.ggnext.contentsystem.ContentSystem
import de.ggnext.contentsystem.cache.ValueCache
import de.ggnext.contentsystem.value.types.SkillPath
import de.ggnext.contentsystem.value.types.SkillPathValue
import de.ggnext.contentsystem.value.types.SkillTier
import kotlinx.coroutines.launch
import org.bson.Document
import kotlin.reflect.KProperty

class SkillPathStore(
    private val key: String,
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): SkillPath =
        runCatching {
            ValueCache.getTyped<SkillPathValue>(key).value
        }.getOrElse {
            val defaultSkillPath =
                SkillPath(
                    key,
                    listOf(SkillTier(2, mapOf("maxFuel" to 50.0))),
                )
            val defaultSkillTierDoc =
                Document()
                    .append("cost", 2)
                    .append("effects", Document("maxFuel", 50.0))
            val doc =
                Document()
                    .append("_id", key)
                    .append("type", "SKILL_PATH")
                    .append("value", listOf(defaultSkillTierDoc))
            ContentSystem.instance.scope.launch {
                ContentSystem.instance.mongoManager.collection
                    .insertOne(doc)
            }
            defaultSkillPath
        }
}