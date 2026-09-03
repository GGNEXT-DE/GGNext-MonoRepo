package eu.ggnext.railway.trade

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.createFiller
import eu.ggnext.core.utils.language
import eu.ggnext.core.utils.name
import eu.ggnext.core.utils.toPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class TradeGui(
    private val plugin: JavaPlugin,
    private val tradeManager: TradeManager,
) {
    fun openTrade(session: TradeSession) {
        plugin.launch {
            openSinglePlayerGui(
                self = session.player1,
                other = session.player2,
                session = session,
            )

            openSinglePlayerGui(
                self = session.player2,
                other = session.player1,
                session = session,
            )
        }
    }

    private suspend fun openSinglePlayerGui(
        self: TradePlayer,
        other: TradePlayer,
        session: TradeSession,
    ) {
        val viewer = self.playerUUID.toPlayer() ?: return

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
                            other.playerUUID.toPlayer()?.name ?: "",
                        ),
                    )
                }

                withTransform(
                    session.player1.accepted,
                    session.player2.accepted,
                    session.offerChangedProperty,
                ) { pane, _ ->

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

                        if (column == 4 || row == 4) {
                            pane[row, column] =
                                StaticElement(
                                    drawable(
                                        createFiller(),
                                    ),
                                )
                        }
                    }

                    val ownStatus =
                        if (self.accepted.value) {
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
                                ItemStack(if (self.accepted.value) Material.GREEN_WOOL else Material.RED_WOOL).name(ownStatus),
                            ),
                        ) {
                            self.accepted.value = !self.accepted.value

                            if (session.player1 == self) {
                                session.player1.accepted = self.accepted
                            } else {
                                session.player2.accepted = self.accepted
                            }

                            checkTradeCompletion(
                                session,
                            )
                        }

                    val partnerStatus =
                        Component
                            .text(
                                "${other.playerUUID.toPlayer()?.name}: ",
                            ).color(
                                NamedTextColor.GREEN,
                            ).append(
                                if (other.accepted.value) {
                                    ready.get(viewer.language())
                                } else {
                                    notReady.get(viewer.language())
                                },
                            )

                    pane[5, 6] =
                        StaticElement(
                            drawable(
                                ItemStack(
                                    if (other.accepted.value) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE,
                                ).name(partnerStatus),
                            ),
                        )

                    forEachInGrid(6, 9) { row, column ->

                        if (!pane.has(row, column)) {
                            pane[row, column] =
                                StaticElement(
                                    drawable(
                                        createFiller(Material.GRAY_STAINED_GLASS_PANE),
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
        if (!session.player1.accepted.value || !session.player2.accepted.value) {
            return
        }

        val p1 = session.player1.playerUUID.toPlayer() ?: return
        val p2 = session.player2.playerUUID.toPlayer() ?: return

        val p1SimulatedInv =
            p1.inventory.storageContents
                .map { it?.clone() }
                .toTypedArray()
        val p2SimulatedInv =
            p2.inventory.storageContents
                .map { it?.clone() }
                .toTypedArray()

        val p1Temp =
            org.bukkit.Bukkit
                .createInventory(null, 36)
                .apply { storageContents = p1SimulatedInv }
        val p2Temp =
            org.bukkit.Bukkit
                .createInventory(null, 36)
                .apply { storageContents = p2SimulatedInv }

        val p1Overflow =
            p1Temp.addItem(
                *session.player2.offer
                    .map { it.clone() }
                    .toTypedArray(),
            )
        val p2Overflow =
            p2Temp.addItem(
                *session.player1.offer
                    .map { it.clone() }
                    .toTypedArray(),
            )

        if (p1Overflow.isNotEmpty() || p2Overflow.isNotEmpty()) {
            val msg by TranslationStore("translations.railway.trade.overflow")
            p1.sendMessage(msg.get(p1.language()))
            p2.sendMessage(msg.get(p2.language()))
            tradeManager.endSession(
                session,
            )

            session.player1.playerUUID
                .toPlayer()
                ?.closeInventory()
            session.player2.playerUUID
                .toPlayer()
                ?.closeInventory()
            return
        }

        session.player2.offer.forEach { p1.inventory.addItem(it.clone()) }
        session.player1.offer.forEach { p2.inventory.addItem(it.clone()) }

        val success by TranslationStore(
            "translations.railway.trade.success",
        )

        session.player1.playerUUID.toPlayer()?.let {
            it.sendMessage(success.get(it.language()))
        }

        session.player2.playerUUID.toPlayer()?.let {
            it.sendMessage(success.get(it.language()))
        }

        session.player1.offer.clear()
        session.player2.offer.clear()

        tradeManager.endSession(
            session,
        )

        session.player1.playerUUID
            .toPlayer()
            ?.closeInventory()
        session.player2.playerUUID
            .toPlayer()
            ?.closeInventory()
    }
}
