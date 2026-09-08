package eu.ggnext.contentsystem.updater

import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import eu.ggnext.contentsystem.value.types.ConfigValue
import eu.ggnext.contentsystem.value.types.EffectType
import eu.ggnext.contentsystem.value.types.MaterialValue
import eu.ggnext.contentsystem.value.types.NumberValue
import eu.ggnext.contentsystem.value.types.Quest
import eu.ggnext.contentsystem.value.types.QuestCategory
import eu.ggnext.contentsystem.value.types.QuestTrackingType
import eu.ggnext.contentsystem.value.types.QuestValue
import eu.ggnext.contentsystem.value.types.SkillPath
import eu.ggnext.contentsystem.value.types.SkillPathValue
import eu.ggnext.contentsystem.value.types.SkillTier
import eu.ggnext.contentsystem.value.types.StringValue
import eu.ggnext.contentsystem.value.types.Translation
import eu.ggnext.contentsystem.value.types.TranslationValue
import org.bson.Document
import org.bukkit.Material
import kotlin.collections.emptyList

/**
 * Maps a MongoDB [Document] to the corresponding [ConfigValue] subtype.
 * The document must contain an `_id` (used as key), a `type`, and a `value` field.
 */
internal object ConfigValueMapper {
    /**
     * Returns `null` (and logs a warning) instead of throwing if [doc] cannot be mapped,
     * so a single malformed value cannot take down the whole plugin.
     */
    fun fromDoc(doc: Document): ConfigValue<*>? {
        val key = doc.getString("_id") ?: "<unknown>"
        return runCatching { mapValue(key, doc) }
            .onFailure { e ->
                log.warn("Failed to map ContentSystem value '$key' (type=${doc.getString("type")}): ${e.message}")
            }.getOrNull()
    }

    private fun mapValue(
        key: String,
        doc: Document,
    ): ConfigValue<*> =
        when (val type = doc.getString("type")) {
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
                            effects =
                                effectsDoc.entries.associate { (k, v) ->
                                    EffectType.fromConfigKey(k) to (v as Number).toDouble()
                                },
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
