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
    private val tierCostLore by TranslationStore("translations.railway.skilltree.detail.cost")
    private val tierEffectsLore by TranslationStore("translations.railway.skilltree.detail.effects")
    private val cannotAffordMessage by TranslationStore("translations.railway.skilltree.detail.cannot_afford")
    private val tierUnlockedMessage by TranslationStore("translations.railway.skilltree.detail.unlocked")

    private val navigating = ConcurrentHashMap.newKeySet<UUID>()

    fun consumeNavigating(player: Player): Boolean = navigating.remove(player.uniqueId)

    suspend fun openPathDetailGui(
        player: Player,
        pathId: String,
    ) {
        val profile =
            profileManager.getActiveProfile(player)
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
                    forEachInGrid(6, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    skillPath.tiers.forEachIndexed { tierIndex, tier ->
                        val row = (tierIndex / 7) + 1
                        val column = (tierIndex % 7) + 1

                        if (row >= 5) return@forEachIndexed // Don't overflow

                        val tierNumber = tierIndex + 1
                        val isUnlocked = tierIndex < unlockedCount
                        val isNextPurchasable = tierIndex == unlockedCount
                        val canAfford = skillTreeManager.canUnlock(profile, pathId)

                        val material =
                            when {
                                isUnlocked -> Material.GREEN_STAINED_GLASS
                                isNextPurchasable -> Material.GOLD_BLOCK
                                else -> Material.GRAY_STAINED_GLASS
                            }

                        val itemName =
                            if (isUnlocked) {
                                Component.text("Tier $tierNumber", NamedTextColor.GREEN)
                            } else if (isNextPurchasable) {
                                Component.text("Tier $tierNumber", NamedTextColor.YELLOW)
                            } else {
                                Component.text("Tier $tierNumber", NamedTextColor.DARK_GRAY)
                            }

                        val lore = mutableListOf<Component>()

                        // Add cost
                        lore.add(Component.text("Cost: ${tier.cost} SP", NamedTextColor.GRAY))

                        // Add effects
                        if (tier.effects.isNotEmpty()) {
                            lore.add(Component.empty())
                            tier.effects.forEach { (effectType, value) ->
                                val effectName =
                                    when (effectType) {
                                        EffectType.MAX_FUEL -> "Max Fuel"
                                        EffectType.MAX_HEALTH -> "Max Health"
                                    }
                                lore.add(
                                    Component.text(
                                        "+ $effectName: +${value.toLong()}",
                                        NamedTextColor.AQUA,
                                    ),
                                )
                            }
                        }

                        pane[row, column] =
                            StaticElement(
                                drawable(
                                    ItemStack(material)
                                        .name(itemName)
                                        .description(lore),
                                ),
                            ) {
                                if (isNextPurchasable) {
                                    plugin.launch {
                                        val success =
                                            skillTreeManager.unlock(profile, pathId)

                                        if (success) {
                                            // Re-fetch the profile from the database to get updated data
                                            val updatedProfile =
                                                profileManager.getProfile(profile.id)
                                            if (updatedProfile != null) {
                                                profileManager.setActiveProfile(player, updatedProfile)
                                                player.inventory.close()
                                                openPathDetailGui(player, pathId)
                                            }
                                        } else {
                                            player.sendMessage(
                                                cannotAffordMessage.get(
                                                    player.language(),
                                                ),
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
