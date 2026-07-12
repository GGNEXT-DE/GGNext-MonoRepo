package de.ggnext.railway.trade

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.language
import de.ggnext.core.utils.name
import de.ggnext.railway.profile.RailwayProfileManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class TradeGui(
    private val plugin: JavaPlugin,
    private val tradeManager: TradeManager,
) {
    private fun createItem(
        material: Material,
        name: Component? = null,
        amount: Int = 1,
    ): ItemStack {
        val item = ItemStack(material, amount)

        if (name != null) {
            item.name(name)
        }

        return item
    }

    private fun createGlass(material: Material): ItemStack = ItemStack(material).name(Component.text(""))

    fun openTrade(session: TradeSession) {
        plugin.launch {
            openSinglePlayerGui(
                viewer = session.player1.player,
                self = session.player1,
                other = session.player2,
                session = session,
            )

            openSinglePlayerGui(
                viewer = session.player2.player,
                self = session.player2,
                other = session.player1,
                session = session,
            )
        }
    }

    private suspend fun openSinglePlayerGui(
        viewer: Player,
        self: TradePlayer,
        other: TradePlayer,
        session: TradeSession,
    ) {
        val title by TranslationStore(
            "translations.railway.trade.gui.title",
        )

        val ready by TranslationStore(
            "translations.railway.trade.gui.ready",
        )

        val notReady by TranslationStore(
            "translations.railway.trade.gui.not_ready",
        )

        val tradeGui =
            buildChestInterface {
                rows = 6

                titleSupplier = {
                    title.get(
                        viewer.language(),
                        listOf(
                            other.player.name,
                        ),
                    )
                }

                withTransform(
                    session.accepted1Property,
                    session.accepted2Property,
                    session.offerChangedProperty,
                ) { pane, view ->

                    self.offer.forEachIndexed { index, item ->

                        val row = index / 4
                        val column = index % 4

                        pane[row, column] =
                            StaticElement(
                                drawable(item),
                            ) {
                                val removed = self.offer.removeAt(index)

                                viewer.inventory.addItem(
                                    removed.clone(),
                                )

                                session.triggerOfferUpdate()
                            }
                    }

                    other.offer.forEachIndexed { index, item ->

                        val row = index / 4
                        val column = index % 4 + 5

                        pane[row, column] =
                            StaticElement(
                                drawable(item),
                            )
                    }

                    forEachInGrid(6, 9) { row, column ->

                        if (column == 4 || row == 3) {
                            pane[row, column] =
                                StaticElement(
                                    drawable(
                                        createGlass(
                                            Material.BLACK_STAINED_GLASS_PANE,
                                        ),
                                    ),
                                )
                        }
                    }

                    val ownStatus =
                        if (self.accept) {
                            ready.get(
                                viewer.language(),
                            )
                        } else {
                            notReady.get(
                                viewer.language(),
                            )
                        }

                    pane[5, 2] =
                        StaticElement(
                            drawable(
                                createItem(
                                    if (self.accept) {
                                        Material.GREEN_WOOL
                                    } else {
                                        Material.RED_WOOL
                                    },
                                    ownStatus,
                                ),
                            ),
                        ) {
                            self.accept =
                                !self.accept

                            if (session.player1 == self) {
                                session.accepted1 =
                                    self.accept
                            } else {
                                session.accepted2 =
                                    self.accept
                            }

                            checkTradeCompletion(
                                session,
                            )
                        }

                    val partnerStatus =
                        Component
                            .text(
                                "${other.player.name}: ",
                            ).color(
                                NamedTextColor.GREEN,
                            ).append(
                                if (other.accept) {
                                    ready.get(
                                        viewer.language(),
                                    )
                                } else {
                                    notReady.get(
                                        viewer.language(),
                                    )
                                },
                            )

                    pane[5, 6] =
                        StaticElement(
                            drawable(
                                createItem(
                                    if (other.accept) {
                                        Material.LIME_STAINED_GLASS_PANE
                                    } else {
                                        Material.RED_STAINED_GLASS_PANE
                                    },
                                    partnerStatus,
                                ),
                            ),
                        )

                    forEachInGrid(6, 9) { row, column ->

                        if (!pane.has(row, column)) {
                            pane[row, column] =
                                StaticElement(
                                    drawable(
                                        createGlass(
                                            Material.GRAY_STAINED_GLASS_PANE,
                                        ),
                                    ),
                                )
                        }
                    }
                }
            }

        tradeGui.open(
            viewer,
            parent = null,
            reload = false,
        )
    }

    private fun checkTradeCompletion(session: TradeSession) {
        if (
            !session.accepted1 ||
            !session.accepted2
        ) {
            return
        }

        plugin.launch {
            session.player1.offer.forEach {
                session.player2.player.inventory.addItem(
                    it.clone(),
                )
            }

            session.player2.offer.forEach {
                session.player1.player.inventory.addItem(
                    it.clone(),
                )
            }

            val success by TranslationStore(
                "translations.railway.trade.success",
            )

            session.player1.player.sendMessage(
                success.get(
                    session.player1.player.language(),
                ),
            )

            session.player2.player.sendMessage(
                success.get(
                    session.player2.player.language(),
                ),
            )

            session.player1.offer.clear()
            session.player2.offer.clear()

            tradeManager.endSession(
                session,
            )

            session.player1.player.closeInventory()
            session.player2.player.closeInventory()
        }
    }
}
