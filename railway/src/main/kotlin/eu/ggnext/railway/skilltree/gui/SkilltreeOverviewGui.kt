package eu.ggnext.railway.skilltree.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import eu.ggnext.contentsystem.value.store.SkillPathStore
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.createFiller
import eu.ggnext.core.utils.description
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.name
import eu.ggnext.railway.profile.RailwayProfileManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SkilltreeOverviewGui(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val skillPathDetailGui: SkillPathDetailGui,
) {
    private lateinit var menuGui: MenuGui

    fun setMenuGui(gui: MenuGui) {
        this.menuGui = gui
    }

    private val backItemName by TranslationStore("translations.railway.skilltree.overview.back.name")
    private val noActiveProfileError by TranslationStore("translations.railway.skilltree.overview.error.no_active_profile")
    private val couldNotLoadProfileError by TranslationStore("translations.railway.skilltree.overview.error.could_not_load_profile")

    private val navigating = ConcurrentHashMap.newKeySet<UUID>()

    fun consumeNavigating(player: Player): Boolean = navigating.remove(player.uniqueId)

    /**
     * Gets icon and description for the given skill path ID
     */
    private fun getPathIcon(pathId: String): Material =
        when (pathId.lowercase()) {
            "motor" -> Material.DIAMOND_PICKAXE
            "speicher" -> Material.BARREL
            "zugkraft" -> Material.MINECART
            else -> Material.BLUE_DYE
        }

    /**
     * Gets translation keys for path name and description
     */
    private fun getPathTranslationKeys(pathId: String): Pair<String, String> =
        Pair(
            "translations.railway.skilltree.overview.path.$pathId.name",
            "translations.railway.skilltree.overview.path.$pathId.description",
        )

    suspend fun openSkilltreeOverviewGui(player: Player) {
        val activeProfileId =
            profileManager.getActiveProfileId(player)
        if (activeProfileId == null) {
            player.sendMessage(
                noActiveProfileError.get(player.language()).color(NamedTextColor.RED),
            )
            return
        }
        val profile =
            profileManager.getProfile(activeProfileId)
        if (profile == null) {
            player.sendMessage(
                couldNotLoadProfileError.get(player.language()).color(NamedTextColor.RED),
            )
            return
        }

        val title by TranslationStore("translations.railway.skilltree.overview.gui.title")

        val skilltreeOverviewGui =
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

                    // Get all skill paths from cache
                    val allSkillPaths = SkillPathStore.getAllSkillPaths()

                    allSkillPaths.forEachIndexed { index, skillPath ->
                        val row = (index / 7) + 1
                        val column = (index % 7) + 1

                        if (row >= 5) return@forEachIndexed // Don't overflow

                        val icon = getPathIcon(skillPath.id)
                        val (nameKey, descKey) = getPathTranslationKeys(skillPath.id)
                        val pathNameTranslation by TranslationStore(nameKey)
                        val pathDescTranslation by TranslationStore(descKey)
                        val pathName = pathNameTranslation.get(player.language())
                        val pathDescription = pathDescTranslation.get(player.language())

                        val unlockedCount = profile.level.unlockedTiers[skillPath.id] ?: 0
                        val totalTiers = skillPath.tiers.size
                        val progressLoreTranslation by TranslationStore("translations.railway.skilltree.overview.progress")
                        val progressLore =
                            progressLoreTranslation.get(
                                player.language(),
                                listOf("$unlockedCount", "$totalTiers"),
                            )

                        pane[row, column] =
                            StaticElement(
                                drawable(
                                    ItemStack(icon)
                                        .name(pathName)
                                        .description(
                                            listOf(
                                                pathDescription.color(NamedTextColor.GRAY),
                                                Component.empty(),
                                                progressLore.color(NamedTextColor.YELLOW),
                                            ),
                                        ),
                                ),
                            ) {
                                plugin.launch {
                                    navigating.add(player.uniqueId)
                                    player.inventory.close()
                                    skillPathDetailGui.openPathDetailGui(player, skillPath.id)
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
                                menuGui.openMenuGui(player)
                            }
                        }
                }
            }
        skilltreeOverviewGui.open(player)
    }
}
