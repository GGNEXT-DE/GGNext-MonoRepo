package eu.ggnext.railway.fuel.gui

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
import eu.ggnext.railway.fuel.AfterburnerActivationResult
import eu.ggnext.railway.fuel.FuelManager
import eu.ggnext.railway.profile.RailwayProfile
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.skilltree.gui.MenuGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FuelGui(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val fuelManager: FuelManager,
) {
    private lateinit var menuGui: MenuGui

    fun setMenuGui(gui: MenuGui) {
        this.menuGui = gui
    }

    private val backItemName by TranslationStore("translations.railway.fuel.gui.back.name")
    private val statusItemName by TranslationStore("translations.railway.fuel.gui.status.name")
    private val regenItemName by TranslationStore("translations.railway.fuel.gui.regen.name")
    private val regenLabel by TranslationStore("translations.railway.fuel.gui.regen.label")
    private val afterburnerItemName by TranslationStore("translations.railway.fuel.gui.afterburner.name")
    private val afterburnerLockedLore by TranslationStore("translations.railway.fuel.gui.afterburner.state.locked")
    private val afterburnerReadyLore by TranslationStore("translations.railway.fuel.gui.afterburner.state.ready")
    private val afterburnerActiveLore by TranslationStore("translations.railway.fuel.gui.afterburner.state.active")
    private val afterburnerCooldownLore by TranslationStore("translations.railway.fuel.gui.afterburner.state.cooldown")

    private val confirmTitle by TranslationStore("translations.railway.fuel.afterburner.confirm.title")
    private val confirmWarning by TranslationStore("translations.railway.fuel.afterburner.confirm.warning")
    private val confirmButtonName by TranslationStore("translations.railway.fuel.afterburner.confirm.confirm.name")
    private val cancelButtonName by TranslationStore("translations.railway.fuel.afterburner.confirm.cancel.name")
    private val activatedMessage by TranslationStore("translations.railway.fuel.afterburner.activated")
    private val cannotActivateMessage by TranslationStore("translations.railway.fuel.afterburner.error.cannot_activate")

    private val navigating = ConcurrentHashMap.newKeySet<UUID>()

    fun consumeNavigating(player: Player): Boolean = navigating.remove(player.uniqueId)

    private fun createProgressBar(
        current: Double,
        max: Double,
    ): String {
        val length = 20
        val percent = if (max > 0) (current / max).coerceIn(0.0, 1.0) else 0.0
        val filled = (percent * length).toInt().coerceIn(0, length)
        return "█".repeat(filled) + "░".repeat(length - filled)
    }

    suspend fun openFuelGui(player: Player) {
        val activeProfileId = profileManager.getActiveProfileId(player) ?: return
        val profile = profileManager.getProfile(activeProfileId) ?: return

        val title by TranslationStore("translations.railway.fuel.gui.title")

        val currentFuel = fuelManager.currentFuel(profile)
        val maxFuel = fuelManager.effectiveMaxFuel(profile)
        val regenSeconds = fuelManager.effectiveRegenSeconds(profile)
        val afterburnerState = profile.fuel.afterburner
        val now = System.currentTimeMillis()

        val isUnlocked = fuelManager.isAfterburnerUnlocked(profile)
        val isActive = afterburnerState.chargesRemaining > 0 && (afterburnerState.windowEndsAt?.let { now < it } == true)
        val isOnCooldown = afterburnerState.cooldownUntil?.let { now < it } == true

        val fuelGui =
            buildChestInterface {
                rows = 3

                titleSupplier = {
                    title.get(player.language())
                }

                withTransform { pane, _ ->
                    forEachInGrid(3, 9) { row, column ->
                        pane[row, column] = StaticElement(drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)))
                    }

                    pane[0, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.LAVA_BUCKET)
                                    .name(statusItemName.get(player.language()))
                                    .description(
                                        listOf(
                                            Component.text(
                                                "${currentFuel.toInt()} / ${maxFuel.toInt()}",
                                                NamedTextColor.AQUA,
                                            ),
                                            Component.text(createProgressBar(currentFuel, maxFuel), NamedTextColor.GOLD),
                                        ),
                                    ),
                            ),
                        )

                    pane[1, 2] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.HOPPER)
                                    .name(regenItemName.get(player.language()))
                                    .description(
                                        listOf(regenLabel.get(player.language(), listOf(regenSeconds.toInt().toString()))),
                                    ),
                            ),
                        )

                    val (afterburnerMaterial, afterburnerLore) =
                        when {
                            !isUnlocked -> {
                                Material.GRAY_DYE to listOf(afterburnerLockedLore.get(player.language()))
                            }

                            isActive -> {
                                val minutesLeft = (checkNotNull(afterburnerState.windowEndsAt) - now) / 60_000L
                                Material.BLAZE_POWDER to
                                    listOf(
                                        afterburnerActiveLore.get(
                                            player.language(),
                                            listOf(afterburnerState.chargesRemaining.toString(), minutesLeft.toString()),
                                        ),
                                    )
                            }

                            isOnCooldown -> {
                                val minutesLeft = (afterburnerState.cooldownUntil - now) / 60_000L
                                Material.RED_DYE to
                                    listOf(afterburnerCooldownLore.get(player.language(), listOf(minutesLeft.toString())))
                            }

                            else -> {
                                Material.BLAZE_POWDER to listOf(afterburnerReadyLore.get(player.language()))
                            }
                        }

                    pane[1, 6] =
                        StaticElement(
                            drawable(
                                ItemStack(afterburnerMaterial)
                                    .name(afterburnerItemName.get(player.language()))
                                    .description(afterburnerLore),
                            ),
                        ) {
                            if (isUnlocked && !isActive && !isOnCooldown) {
                                plugin.launch {
                                    navigating.add(player.uniqueId)
                                    player.inventory.close()
                                    openAfterburnerConfirmGui(player, profile)
                                }
                            }
                        }

                    pane[2, 0] =
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
        fuelGui.open(player)
    }

    suspend fun openAfterburnerConfirmGui(
        player: Player,
        profile: RailwayProfile,
    ) {
        val preview = fuelManager.previewAfterburner(profile)

        val confirmGui =
            buildChestInterface {
                rows = 3

                titleSupplier = {
                    confirmTitle.get(player.language())
                }

                withTransform { pane, _ ->
                    forEachInGrid(3, 9) { row, column ->
                        pane[row, column] = StaticElement(drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)))
                    }

                    pane[0, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.BLAZE_POWDER)
                                    .name(afterburnerItemName.get(player.language()))
                                    .description(
                                        listOf(
                                            confirmWarning.get(
                                                player.language(),
                                                listOf(
                                                    (preview?.charges ?: 0).toString(),
                                                    (preview?.windowMinutes ?: 0).toString(),
                                                    (preview?.cooldownMinutes ?: 0).toString(),
                                                ),
                                            ),
                                        ),
                                    ),
                            ),
                        )

                    pane[1, 3] =
                        StaticElement(
                            drawable(ItemStack(Material.RED_WOOL).name(cancelButtonName.get(player.language()))),
                        ) {
                            plugin.launch {
                                navigating.add(player.uniqueId)
                                player.inventory.close()
                                openFuelGui(player)
                            }
                        }

                    pane[1, 5] =
                        StaticElement(
                            drawable(ItemStack(Material.GREEN_WOOL).name(confirmButtonName.get(player.language()))),
                        ) {
                            plugin.launch {
                                when (val result = fuelManager.activateAfterburner(profile)) {
                                    is AfterburnerActivationResult.Success -> {
                                        player.sendMessage(
                                            activatedMessage.get(
                                                player.language(),
                                                listOf(
                                                    result.charges.toString(),
                                                    result.windowMinutes.toString(),
                                                    result.cooldownMinutes.toString(),
                                                ),
                                            ),
                                        )
                                    }

                                    else -> {
                                        player.sendMessage(cannotActivateMessage.get(player.language()))
                                    }
                                }
                                player.inventory.close()
                            }
                        }
                }
            }
        confirmGui.open(player)
    }
}
