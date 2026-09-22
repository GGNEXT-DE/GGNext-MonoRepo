package eu.ggnext.railway.skilltree.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.createFiller
import eu.ggnext.core.utils.description
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.name
import eu.ggnext.railway.fuel.gui.FuelGui
import eu.ggnext.railway.profile.RailwayProfileManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class MenuGui(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val skilltreeOverviewGui: SkilltreeOverviewGui,
) {
    private lateinit var fuelGui: FuelGui

    fun setFuelGui(gui: FuelGui) {
        this.fuelGui = gui
    }

    private val menuTitle by TranslationStore("translations.railway.menu.title")
    private val dollarsItemName by TranslationStore("translations.railway.menu.dollars.name")
    private val levelItemName by TranslationStore("translations.railway.menu.level.name")
    private val levelLabel by TranslationStore("translations.railway.menu.level.label")
    private val xpLabel by TranslationStore("translations.railway.menu.level.xp_label")
    private val skillPointsItemName by TranslationStore("translations.railway.menu.skill_points.name")
    private val skilltreeItemName by TranslationStore("translations.railway.menu.skilltree.name")
    private val fuelItemName by TranslationStore("translations.railway.menu.fuel.name")

    suspend fun openMenuGui(player: Player) {
        val activeProfileId =
            profileManager.getActiveProfileId(player)
                ?: return
        val profile =
            profileManager.getProfile(activeProfileId)
                ?: return

        val title by TranslationStore("translations.railway.menu.title")

        val menuGui =
            buildChestInterface {
                rows = 5

                titleSupplier = {
                    title.get(player.language())
                }

                withTransform { pane, _ ->
                    forEachInGrid(5, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    // Dollars display
                    pane[1, 1] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.GOLD_INGOT)
                                    .name(dollarsItemName.get(player.language()))
                                    .description(
                                        listOf(
                                            Component.text(profile.railwayDollars.toLong()),
                                        ),
                                    ),
                            ),
                        )

                    // Level display
                    val currentLevel = profile.level.currentLevel
                    val currentXp = profile.level.xp
                    val xpForCurrentLevel = profile.level.neededXpForLevel(currentLevel)
                    val xpForNextLevel = profile.level.neededXpForLevel(currentLevel + 1)
                    val xpProgress = currentXp - xpForCurrentLevel
                    val xpNeeded = xpForNextLevel - xpForCurrentLevel
                    val progressPercent = if (xpNeeded > 0) (xpProgress * 100 / xpNeeded).toInt() else 0

                    pane[1, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.EXPERIENCE_BOTTLE)
                                    .name(levelItemName.get(player.language()))
                                    .description(
                                        listOf(
                                            levelLabel
                                                .get(player.language())
                                                .append(Component.text(": $currentLevel", NamedTextColor.AQUA)),
                                            xpLabel
                                                .get(player.language())
                                                .append(
                                                    Component.text(": $xpProgress / $xpNeeded ($progressPercent%)", NamedTextColor.GRAY),
                                                ),
                                        ),
                                    ),
                            ),
                        )

                    // Skill points display
                    pane[1, 7] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.EMERALD)
                                    .name(skillPointsItemName.get(player.language()))
                                    .description(
                                        listOf(
                                            Component.text(profile.level.skillPoints),
                                        ),
                                    ),
                            ),
                        )

                    // Skilltree button
                    pane[3, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.NETHER_STAR)
                                    .name(skilltreeItemName.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                player.inventory.close()
                                skilltreeOverviewGui.openSkilltreeOverviewGui(player)
                            }
                        }

                    pane[3, 6] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.LAVA_BUCKET)
                                    .name(fuelItemName.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                player.inventory.close()
                                fuelGui.openFuelGui(player)
                            }
                        }
                }
            }
        menuGui.open(player)
    }
}
