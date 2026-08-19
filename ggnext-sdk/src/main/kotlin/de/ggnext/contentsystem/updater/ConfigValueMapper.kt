package de.ggnext.contentsystem.updater

import de.ggnext.contentsystem.value.types.ConfigValue
import de.ggnext.contentsystem.value.types.MaterialValue
import de.ggnext.contentsystem.value.types.NumberValue
import de.ggnext.contentsystem.value.types.Quest
import de.ggnext.contentsystem.value.types.QuestCategory
import de.ggnext.contentsystem.value.types.QuestTrackingType
import de.ggnext.contentsystem.value.types.QuestValue
import de.ggnext.contentsystem.value.types.SkillPath
import de.ggnext.contentsystem.value.types.SkillPathValue
import de.ggnext.contentsystem.value.types.SkillTier
import de.ggnext.contentsystem.value.types.StringValue
import de.ggnext.contentsystem.value.types.Translation
import de.ggnext.contentsystem.value.types.TranslationValue
import org.bson.Document
import org.bukkit.Material
import kotlin.collections.emptyList

/**
 * Maps a MongoDB [Document] to the corresponding [ConfigValue] subtype.
 * The document must contain an `_id` (used as key), a `type`, and a `value` field.
 */
internal object ConfigValueMapper {
    fun fromDoc(doc: Document): ConfigValue<*> {
        val key = doc.getString("_id")
        return when (val type = doc.getString("type")) {
            "NUMBER" -> {
                NumberValue(
                    key = key,
                    value = doc.getInteger("value"),
                )
            }

            "STRING" -> {
                StringValue(
                    key = key,
                    value = doc.getString("value"),
                )
            }

            "MATERIAL" -> {
                MaterialValue(
                    key = key,
                    value = Material.valueOf(doc.getString("value")),
                )
            }

            "TRANSLATION" -> {
                val valueDoc = doc.get("value", Document::class.java)
                TranslationValue(
                    key = key,
                    value =
                        Translation(
                            de = valueDoc.getString("de"),
                            en = valueDoc.getString("en"),
                        ),
                )
            }

            "QUEST" -> {
                val valueDoc = doc.get("value", Document::class.java)
                val nameDoc = valueDoc.get("name", Document::class.java)
                val descDoc = valueDoc.get("description", Document::class.java)
                QuestValue(
                    key = key,
                    value =
                        Quest(
                            name =
                                Translation(
                                    de = nameDoc.getString("de"),
                                    en = nameDoc.getString("en"),
                                ),
                            description =
                                Translation(
                                    de = descDoc.getString("de"),
                                    en = descDoc.getString("en"),
                                ),
                            category = QuestCategory.valueOf(valueDoc.getString("category")),
                            trackingType = QuestTrackingType.valueOf(valueDoc.getString("trackingType")),
                            trackingTarget = valueDoc.getString("trackingTarget"),
                            targetValue = valueDoc.getInteger("targetValue"),
                            rewardXp = valueDoc.getInteger("rewardXp"),
                            rewardMoney = valueDoc.getDouble("rewardMoney"),
                        ),
                )
            }

            "SKILL_PATH" -> {
                val tierDocs = doc.getList("value", Document::class.java) ?: emptyList()
                val tiers =
                    tierDocs.map { tierDoc ->
                        val effectsDoc = tierDoc.get("effects", Document::class.java) ?: Document()
                        SkillTier(
                            cost = tierDoc.getInteger("cost"),
                            effects = effectsDoc.mapValues { (_, v) -> (v as Number).toDouble() },
                        )
                    }
                SkillPathValue(
                    key = key,
                    value = SkillPath(key, tiers),
                )
            }

            else -> {
                error("Unknown type: $type")
            }
        }
    }
}
