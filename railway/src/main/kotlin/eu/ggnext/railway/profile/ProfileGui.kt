package eu.ggnext.railway.profile

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
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ProfileGui(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
) {
    private val previousGuiItemName by TranslationStore("translations.previous.gui")

    private val navigating = ConcurrentHashMap.newKeySet<UUID>()

    fun consumeNavigating(player: Player): Boolean = navigating.remove(player.uniqueId)

    fun clearNavigating(player: Player) = navigating.remove(player.uniqueId)

    fun reopenProfileGui(player: Player) {
        plugin.launch {
            delay(50)

            if (player.isOnline && profileManager.getActiveProfileId(player) == null) {
                openProfileGui(player)
            }
        }
    }

    suspend fun openProfileGui(player: Player) {
        val title by TranslationStore(
            "translations.railway.profile.gui.title",
        )

        val profileGui =
            buildChestInterface {
                rows = 3

                titleSupplier = {
                    title.get(
                        player.language(),
                    )
                }

                withTransform { pane, _ ->

                    forEachInGrid(3, 9) { row, column ->
                        pane[row, column] = StaticElement(drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)))
                        if (row == 1) {
                            pane[row, column] = StaticElement(drawable(createFiller()))
                        }
                    }

                    val profiles = profileManager.getProfiles(player)

                    profiles.forEachIndexed { index, profile ->
                        val profileStatusTextColor =
                            if (profileManager.getActiveProfileId(player) == profile.id) {
                                NamedTextColor.GREEN
                            } else {
                                NamedTextColor.RED
                            }
                        pane[index / 9, index % 9] =
                            StaticElement(
                                drawable(
                                    ItemStack(
                                        Material.GOLD_BLOCK,
                                    ).name(Component.text(profile.name, profileStatusTextColor))
                                        .description(
                                            listOf(
                                                Component
                                                    .text(
                                                        profile.railwayDollars,
                                                    ),
                                                Component.text(profile.level.xp),
                                                Component.text(profile.level.skillPoints),
                                            ),
                                        ),
                                ),
                            ) { event ->
                                val type = event.type

                                when (type) {
                                    ClickType.LEFT -> {
                                        profileManager.setActiveProfile(player, profile)
                                        player.inventory.close()
                                    }

                                    ClickType.RIGHT -> {
                                        navigating.add(player.uniqueId)
                                        player.inventory.close()
                                        plugin.launch {
                                            openSpecProfileGui(player, profile)
                                        }
                                    }

                                    else -> {}
                                }
                            }
                    }

                    val createItemName by TranslationStore("translations.railway.profile.gui.create.item.name")
                    pane[2, 4] =
                        StaticElement(drawable(ItemStack(Material.NAME_TAG).name(createItemName.get(player.language())))) {
                            plugin.launch {
                                val profile = profileManager.createProfile(player, railwayProfileNames.random())
                                if (profileManager.getActiveProfileId(player) == null && profile != null) {
                                    profileManager.setActiveProfile(player, profile)
                                }
                                player.inventory.close()
                            }
                        }
                }
            }
        profileGui.open(player)
    }

    suspend fun openSpecProfileGui(
        player: Player,
        profile: RailwayProfile,
    ) {
        val title by TranslationStore(
            "translations.railway.profile.gui.spec.title",
        )

        val specProfileGui =
            buildChestInterface {
                rows = 3

                titleSupplier = {
                    title.get(
                        player.language(),
                    )
                }

                withTransform { pane, _ ->

                    forEachInGrid(3, 9) { row, column ->
                        pane[row, column] = StaticElement(drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)))
                        if (row == 1) {
                            pane[row, column] = StaticElement(drawable(createFiller()))
                        }
                    }

                    pane[0, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(
                                    Material.GOLD_BLOCK,
                                ).name(Component.text(profile.name))
                                    .description(
                                        listOf(
                                            Component
                                                .text(
                                                    profile.railwayDollars,
                                                ),
                                            Component.text(profile.level.xp),
                                            Component.text(profile.level.skillPoints),
                                        ),
                                    ),
                            ),
                        )

                    pane[2, 0] =
                        StaticElement(
                            drawable(ItemStack(Material.PAPER).name(previousGuiItemName.get(player.language()))),
                        ) {
                            plugin.launch {
                                navigating.add(player.uniqueId)
                                player.inventory.close()
                                openProfileGui(player)
                            }
                        }

                    val deletionItemName by TranslationStore("translations.railway.profile.gui.spec.deletion.item.name")
                    pane[2, 3] =
                        StaticElement(
                            drawable(ItemStack(Material.BARRIER).name(deletionItemName.get(player.language()))),
                        ) {
                            plugin.launch {
                                navigating.add(player.uniqueId)
                                player.inventory.close()
                                openConfirmProfileGui(player, profile)
                            }
                        }

                    val activateItemName by TranslationStore("translations.railway.profile.gui.spec.activation.item.name")
                    pane[2, 5] =
                        StaticElement(
                            drawable(ItemStack(Material.GREEN_WOOL).name(activateItemName.get(player.language()))),
                        ) {
                            profileManager.setActiveProfile(player, profile)
                            player.inventory.close()
                        }
                }
            }
        specProfileGui.open(player)
    }

    suspend fun openConfirmProfileGui(
        player: Player,
        profile: RailwayProfile,
    ) {
        val title by TranslationStore(
            "translations.railway.profile.gui.confirm.title",
        )

        val confirmProfileGui =
            buildChestInterface {
                rows = 3

                titleSupplier = {
                    title.get(
                        player.language(),
                    )
                }

                withTransform { pane, _ ->

                    forEachInGrid(3, 9) { row, column ->
                        pane[row, column] = StaticElement(drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)))
                    }

                    pane[2, 0] =
                        StaticElement(
                            drawable(ItemStack(Material.PAPER).name(previousGuiItemName.get(player.language()))),
                        ) {
                            plugin.launch {
                                navigating.add(player.uniqueId)
                                player.inventory.close()
                                openProfileGui(player)
                            }
                        }

                    val deletionItemName by TranslationStore("translations.railway.profile.gui.confirm.deletion.item.name")
                    pane[1, 4] =
                        StaticElement(
                            drawable(ItemStack(Material.BARRIER).name(deletionItemName.get(player.language()))),
                        ) {
                            plugin.launch {
                                profileManager.deleteProfile(player, profile.id)
                                player.inventory.close()
                            }
                        }
                }
            }
        confirmProfileGui.open(player)
    }
}
