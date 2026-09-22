package eu.ggnext.railway.skilltree.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import eu.ggnext.contentsystem.value.store.SkillPathStore
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.contentsystem.value.types.EffectType
import eu.ggnext.core.utils.createFiller
import eu.ggnext.core.utils.description
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.name
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.skilltree.SkillTreeManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SkillPathDetailGui(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val skillTreeManager: SkillTreeManager,
) {
    private lateinit var skilltreeOverviewGui: SkilltreeOverviewGui

    fun setSkilltreeOverviewGui(gui: SkilltreeOverviewGui) {
        this.skilltreeOverviewGui = gui
    }

    private val detailTitle by TranslationStore("translations.railway.skilltree.detail.title")
    private val backItemName by TranslationStore("translations.railway.skilltree.detail.back.name")
    private val cannotAffordMessage by TranslationStore("translations.railway.skilltree.detail.cannot_afford")
    private val tierLockedLore by TranslationStore("translations.railway.skilltree.detail.tier.locked")
    private val tierUnlockedLore by TranslationStore("translations.railway.skilltree.detail.tier.unlocked")
    private val tierNextAffordableLore by TranslationStore("translations.railway.skilltree.detail.tier.next.affordable")
    private val tierNextUnaffordableLore by TranslationStore("translations.railway.skilltree.detail.tier.next.unaffordable")
    private val tierCostLabel by TranslationStore("translations.railway.skilltree.detail.tier.cost_label")

    private val navigating = ConcurrentHashMap.newKeySet<UUID>()

    fun consumeNavigating(player: Player): Boolean = navigating.remove(player.uniqueId)

    /**
     * Calculates the position (row, col) for a tier using a snake pattern.
     * Pattern: columns go top→bottom, bottom→top, top→bottom, etc.
     * With 1-slot buffer: rows 1-4, cols 1-7
     */
    private fun getSnakeTierPosition(tierIndex: Int): Pair<Int, Int> {
        val column = (tierIndex / 4) + 1 // Column 1-7
        val isEvenColumn = column % 2 == 1 // Odd columns: top→bottom
        val rowInColumn = tierIndex % 4 // 0-3

        val row =
            if (isEvenColumn) {
                1 + rowInColumn // Rows 1-4
            } else {
                4 - rowInColumn // Rows 4-1
            }

        return Pair(row, column)
    }

    /**
     * Gets the icon for the given SkillPath ID
     */
    private fun getIconForPath(pathId: String): Material =
        when (pathId.lowercase()) {
            "motor" -> Material.DIAMOND_PICKAXE
            "speicher" -> Material.BARREL
            "zugkraft" -> Material.MINECART
            "pumpe" -> Material.HOPPER
            "nachbrenner" -> Material.BLAZE_POWDER
            else -> Material.BLUE_DYE
        }

    /**
     * Gets a displayable name for the effect type
     */
    private fun getEffectTranslationKey(effectType: EffectType): String =
        "translations.railway.skilltree.detail.effects.${effectType.configKey}"

    suspend fun openPathDetailGui(
        player: Player,
        pathId: String,
    ) {
        val activeProfileId =
            profileManager.getActiveProfileId(player)
                ?: return
        val profile =
            profileManager.getProfile(activeProfileId)
                ?: return

        val skillPath by SkillPathStore("skill_path.$pathId")
        val unlockedCount = profile.level.unlockedTiers[pathId] ?: 0

        val title by TranslationStore("translations.railway.skilltree.detail.title")

        val detailGui =
            buildChestInterface {
                rows = 6

                titleSupplier = {
                    title.get(player.language())
                }

                withTransform { pane, _ ->
                    // Fill background
                    forEachInGrid(6, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    val icon = getIconForPath(pathId)

                    skillPath.tiers.forEachIndexed { tierIndex, tier ->
                        val (row, column) = getSnakeTierPosition(tierIndex)

                        if (row < 1 || row > 4 || column < 1 || column > 7) {
                            return@forEachIndexed
                        }

                        val tierNumber = tierIndex + 1
                        val isUnlocked = tierIndex < unlockedCount
                        val isNextPurchasable = tierIndex == unlockedCount
                        val canAfford = skillTreeManager.canUnlock(profile, pathId)

                        // Visual state
                        val material =
                            when {
                                isUnlocked -> Material.GREEN_STAINED_GLASS
                                isNextPurchasable && canAfford -> Material.GOLD_BLOCK
                                isNextPurchasable && !canAfford -> Material.RED_STAINED_GLASS
                                else -> Material.GRAY_STAINED_GLASS
                            }

                        val itemName =
                            when {
                                isUnlocked -> {
                                    Component.text("Tier $tierNumber", NamedTextColor.GREEN)
                                }

                                isNextPurchasable && canAfford -> {
                                    Component.text("Tier $tierNumber", NamedTextColor.YELLOW)
                                }

                                isNextPurchasable && !canAfford -> {
                                    Component.text("Tier $tierNumber", NamedTextColor.RED)
                                }

                                else -> {
                                    Component.text("Tier $tierNumber", NamedTextColor.DARK_GRAY)
                                }
                            }

                        val lore = mutableListOf<Component>()

                        // Cost
                        lore.add(
                            tierCostLabel
                                .get(player.language())
                                .append(Component.text(" ${tier.cost} SP"))
                                .color(
                                    if (profile.level.skillPoints >= tier.cost) {
                                        NamedTextColor.GREEN
                                    } else {
                                        NamedTextColor.RED
                                    },
                                ),
                        )

                        // Effects
                        if (tier.effects.isNotEmpty()) {
                            lore.add(Component.empty())
                            tier.effects.forEach { (effectType, value) ->
                                val effectNameTranslation by
                                    TranslationStore(getEffectTranslationKey(effectType))
                                val displayValue =
                                    when {
                                        effectType == EffectType.ZONE_RARITY -> {
                                            "${value.toInt()}%"
                                        }

                                        effectType == EffectType.FUEL_REGEN_SECONDS -> {
                                            "${value.toLong()}s"
                                        }

                                        effectType == EffectType.AFTERBURNER_COOLDOWN_MINUTES && value == 0.0 -> {
                                            if (player.language() == "de") "Freigeschaltet" else "Unlocked"
                                        }

                                        effectType == EffectType.AFTERBURNER_COOLDOWN_MINUTES -> {
                                            "${value.toLong()}min"
                                        }

                                        else -> {
                                            "+${value.toLong()}"
                                        }
                                    }
                                lore.add(
                                    effectNameTranslation
                                        .get(
                                            player.language(),
                                            listOf(displayValue),
                                        ).color(NamedTextColor.AQUA),
                                )
                            }
                        }

                        // Status
                        lore.add(Component.empty())
                        when {
                            isUnlocked -> {
                                lore.add(
                                    tierUnlockedLore
                                        .get(player.language())
                                        .color(NamedTextColor.GREEN),
                                )
                            }

                            isNextPurchasable -> {
                                lore.add(
                                    (
                                        if (canAfford) {
                                            tierNextAffordableLore.get(player.language())
                                        } else {
                                            tierNextUnaffordableLore.get(player.language())
                                        }
                                    ).color(
                                        if (canAfford) NamedTextColor.YELLOW else NamedTextColor.RED,
                                    ),
                                )
                            }

                            else -> {
                                lore.add(
                                    tierLockedLore
                                        .get(player.language())
                                        .color(NamedTextColor.DARK_GRAY),
                                )
                            }
                        }

                        pane[row, column] =
                            StaticElement(
                                drawable(
                                    ItemStack(icon)
                                        .name(itemName)
                                        .description(lore),
                                ),
                            ) {
                                if (isNextPurchasable && canAfford) {
                                    plugin.launch {
                                        val success =
                                            skillTreeManager.unlock(profile, pathId)

                                        if (success) {
                                            val updatedProfile =
                                                profileManager.getProfile(profile.id)
                                            if (updatedProfile != null) {
                                                profileManager.setActiveProfile(player, updatedProfile)
                                                player.inventory.close()
                                                openPathDetailGui(player, pathId)
                                            }
                                        } else {
                                            player.sendMessage(
                                                cannotAffordMessage.get(player.language()),
                                            )
                                        }
                                    }
                                }
                            }
                    }

                    // Back button
                    pane[5, 0] =
                        StaticElement(
                            drawable(ItemStack(Material.PAPER).name(backItemName.get(player.language()))),
                        ) {
                            plugin.launch {
                                navigating.add(player.uniqueId)
                                player.inventory.close()
                                skilltreeOverviewGui.openSkilltreeOverviewGui(player)
                            }
                        }
                }
            }
        detailGui.open(player)
    }
}
