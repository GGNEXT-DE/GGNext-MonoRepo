package eu.ggnext.contentsystem.value.store

import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.contentsystem.cache.ValueCache
import eu.ggnext.contentsystem.value.types.EffectType
import eu.ggnext.contentsystem.value.types.SkillPath
import eu.ggnext.contentsystem.value.types.SkillPathValue
import eu.ggnext.contentsystem.value.types.SkillTier
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
            // Create default based on the path ID
            val pathId = key.removePrefix("skill_path.")
            when (pathId) {
                "motor" -> {
                    SkillPath(
                        pathId,
                        listOf(
                            SkillTier(5, mapOf(EffectType.ZONE_RARITY to 5.0)),
                            SkillTier(14, mapOf(EffectType.ZONE_RARITY to 10.0)),
                            SkillTier(28, mapOf(EffectType.ZONE_RARITY to 15.0)),
                        ),
                    )
                }

                "speicher" -> {
                    SkillPath(
                        pathId,
                        listOf(
                            SkillTier(5, mapOf(EffectType.MAX_FUEL to 100.0)),
                            SkillTier(14, mapOf(EffectType.MAX_FUEL to 100.0)),
                            SkillTier(28, mapOf(EffectType.MAX_FUEL to 200.0)),
                        ),
                    )
                }

                "zugkraft" -> {
                    SkillPath(
                        pathId,
                        listOf(
                            SkillTier(5, mapOf(EffectType.MAX_WAGONS to 1.0)),
                            SkillTier(14, mapOf(EffectType.MAX_WAGONS to 2.0)),
                            SkillTier(28, mapOf(EffectType.MAX_WAGONS to 3.0)),
                        ),
                    )
                }

                "pumpe" -> {
                    SkillPath(
                        pathId,
                        listOf(
                            SkillTier(5, mapOf(EffectType.FUEL_REGEN_SECONDS to -1.0)),
                            SkillTier(14, mapOf(EffectType.FUEL_REGEN_SECONDS to -2.0)),
                            SkillTier(28, mapOf(EffectType.FUEL_REGEN_SECONDS to -2.0)),
                        ),
                    )
                }

                "nachbrenner" -> {
                    SkillPath(
                        pathId,
                        listOf(
                            SkillTier(5, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to 0.0)),
                            SkillTier(14, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to -30.0)),
                            SkillTier(28, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to -60.0)),
                        ),
                    )
                }

                else -> {
                    SkillPath(
                        pathId,
                        listOf(SkillTier(2, mapOf(EffectType.MAX_FUEL to 50.0))),
                    )
                }
            }
        }

    companion object {
        fun getAllSkillPaths(): List<SkillPath> {
            val cached =
                ValueCache
                    .getByPrefix<SkillPathValue>("skill_path.")
                    .map { it.value }
                    .sortedBy { it.id }

            // If no data is cached, return default sample data
            if (cached.isEmpty()) {
                return listOf(
                    SkillPath(
                        "motor",
                        listOf(
                            SkillTier(5, mapOf(EffectType.ZONE_RARITY to 5.0)),
                            SkillTier(14, mapOf(EffectType.ZONE_RARITY to 10.0)),
                            SkillTier(28, mapOf(EffectType.ZONE_RARITY to 15.0)),
                        ),
                    ),
                    SkillPath(
                        "speicher",
                        listOf(
                            SkillTier(5, mapOf(EffectType.MAX_FUEL to 100.0)),
                            SkillTier(14, mapOf(EffectType.MAX_FUEL to 100.0)),
                            SkillTier(28, mapOf(EffectType.MAX_FUEL to 200.0)),
                        ),
                    ),
                    SkillPath(
                        "zugkraft",
                        listOf(
                            SkillTier(5, mapOf(EffectType.MAX_WAGONS to 1.0)),
                            SkillTier(14, mapOf(EffectType.MAX_WAGONS to 2.0)),
                            SkillTier(28, mapOf(EffectType.MAX_WAGONS to 3.0)),
                        ),
                    ),
                    SkillPath(
                        "pumpe",
                        listOf(
                            SkillTier(5, mapOf(EffectType.FUEL_REGEN_SECONDS to -1.0)),
                            SkillTier(14, mapOf(EffectType.FUEL_REGEN_SECONDS to -2.0)),
                            SkillTier(28, mapOf(EffectType.FUEL_REGEN_SECONDS to -2.0)),
                        ),
                    ),
                    SkillPath(
                        "nachbrenner",
                        listOf(
                            SkillTier(5, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to 0.0)),
                            SkillTier(14, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to -30.0)),
                            SkillTier(28, mapOf(EffectType.AFTERBURNER_COOLDOWN_MINUTES to -60.0)),
                        ),
                    ),
                )
            }

            return cached
        }
    }
}
