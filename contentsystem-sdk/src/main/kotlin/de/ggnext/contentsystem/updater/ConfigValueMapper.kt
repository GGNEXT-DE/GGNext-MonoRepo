package de.ggnext.contentsystem.updater

import de.ggnext.contentsystem.value.types.ConfigValue
import de.ggnext.contentsystem.value.types.MaterialValue
import de.ggnext.contentsystem.value.types.NumberValue
import de.ggnext.contentsystem.value.types.StringValue
import de.ggnext.contentsystem.value.types.Translation
import de.ggnext.contentsystem.value.types.TranslationValue
import org.bson.Document
import org.bukkit.Material

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

            else -> {
                error("Unknown type: $type")
            }
        }
    }
}
