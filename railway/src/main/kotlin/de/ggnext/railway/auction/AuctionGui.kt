package de.ggnext.railway.auction

import com.github.shynixn.mccoroutine.bukkit.launch
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.InterfaceProperty
import com.noxcrew.interfaces.utilities.forEachInGrid
import de.ggnext.contentsystem.value.store.NumberStore
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.core.utils.createFiller
import de.ggnext.core.utils.language
import de.ggnext.core.utils.name
import de.ggnext.railway.profile.RailwayProfileManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class AuctionGui(
    private val plugin: JavaPlugin,
    private val auctionManager: AuctionManager,
    private val profileManager: RailwayProfileManager,
) {
    private val defaultPrice by NumberStore("numbers.railway.auction.default_price")
    private val minimumPrice by NumberStore("numbers.railway.auction.minimum_price")
    private val maximumAuctions by NumberStore("numbers.railway.auction.maximum_auctions")

    private val auctionHouseTitle by TranslationStore("translations.railway.auction.gui.title")
    private val noActiveAuctions by TranslationStore("translations.railway.auction.gui.empty")
    private val auctionPrice by TranslationStore("translations.railway.auction.item.price")
    private val auctionSeller by TranslationStore("translations.railway.auction.item.seller")
    private val buyAuctionAction by TranslationStore("translations.railway.auction.item.buy")
    private val createAuctionText by TranslationStore("translations.railway.auction.create.title")
    private val cancelAction by TranslationStore("translations.railway.auction.create.cancel")
    private val claimTitle by TranslationStore("translations.railway.auction.claim.title")
    private val noClaims by TranslationStore("translations.railway.auction.claim.empty")
    private val boughtStatus by TranslationStore("translations.railway.auction.claim.sold")
    private val expiredStatus by TranslationStore("translations.railway.auction.claim.expired")
    private val claimAction by TranslationStore("translations.railway.auction.claim.collect")
    private val backAction by TranslationStore("translations.railway.auction.back")
    private val previousPageAction by TranslationStore("translations.railway.auction.pagination.previous")
    private val nextPageAction by TranslationStore("translations.railway.auction.pagination.next")
    private val noActiveProfile by TranslationStore("translations.railway.auction.message.no_active_profile")
    private val holdItem by TranslationStore("translations.railway.auction.message.hold_item")
    private val auctionCreated by TranslationStore("translations.railway.auction.message.created")
    private val auctionCreateFailed by TranslationStore("translations.railway.auction.message.create_failed")
    private val ownAuction by TranslationStore("translations.railway.auction.message.own_auction")

    // FIXME(key-consistency): near-duplicate of no_active_profile above; confirm both are intentional.
    private val profileNotFound by TranslationStore("translations.railway.auction.message.profile_not_found")
    private val notEnoughMoney by TranslationStore("translations.railway.auction.message.not_enough_money")
    private val auctionAlreadyBought by TranslationStore("translations.railway.auction.message.already_bought")
    private val auctionBought by TranslationStore("translations.railway.auction.message.bought")
    private val itemLoadFailed by TranslationStore("translations.railway.auction.message.item_load_failed")
    private val inventoryFull by TranslationStore("translations.railway.auction.message.inventory_full")
    private val itemAlreadyClaimed by TranslationStore("translations.railway.auction.message.already_claimed")
    private val itemClaimed by TranslationStore("translations.railway.auction.message.claimed")

    private val configuredMinimumPrice: Long
        get() = minimumPrice.toLong().coerceAtLeast(1L)

    private val configuredDefaultPrice: Long
        get() = defaultPrice.toLong().coerceAtLeast(configuredMinimumPrice)

    private val auctionsPerPage: Int
        get() = maximumAuctions.coerceIn(1, MAX_AUCTIONS_PER_PAGE)

    suspend fun openAuctionGui(
        player: Player,
        page: Int = 0,
    ) {
        val auctionPage = paginate(auctionManager.getActiveAuctions(), page)
        val auctions = auctionPage.entries

        val auctionGui =
            buildChestInterface {
                rows = 6

                titleSupplier = {
                    auctionHouseTitle.get(player.language())
                }

                withTransform { pane, _ ->
                    forEachInGrid(6, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    if (auctions.isEmpty()) {
                        pane[2, 4] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.BARRIER)
                                        .name(noActiveAuctions.get(player.language())),
                                ),
                            )
                    }

                    auctions.forEachIndexed { index, auction ->
                        val item =
                            runCatching {
                                auction.itemStack.deserializeAuctionItem()
                            }.getOrElse {
                                ItemStack(Material.BARRIER)
                            }

                        val sellerName =
                            profileManager.getProfile(auction.sellerId)?.name ?: "Unbekannt"

                        val itemMeta = item.itemMeta
                        val existingLore = itemMeta.lore() ?: emptyList()

                        itemMeta.lore(
                            existingLore +
                                listOf(
                                    Component.empty(),
                                    auctionPrice.get(
                                        player.language(),
                                        listOf(auction.price.toString()),
                                    ),
                                    auctionSeller.get(
                                        player.language(),
                                        listOf(sellerName),
                                    ),
                                    buyAuctionAction.get(player.language()),
                                ),
                        )

                        item.itemMeta = itemMeta

                        pane[index / 9, index % 9] =
                            StaticElement(
                                drawable(item),
                            ) {
                                plugin.launch {
                                    buyAuction(player, auction)
                                }
                            }
                    }

                    if (auctionPage.currentPage > 0) {
                        pane[5, 0] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.ARROW)
                                        .name(previousPageAction.get(player.language())),
                                ),
                            ) {
                                plugin.launch {
                                    player.inventory.close()
                                    openAuctionGui(player, auctionPage.currentPage - 1)
                                }
                            }
                    }

                    pane[5, 3] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.EMERALD)
                                    .name(createAuctionText.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                player.inventory.close()
                                openCreateAuctionGui(player)
                            }
                        }

                    pane[5, 5] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.ENDER_CHEST)
                                    .name(claimTitle.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                player.inventory.close()
                                openClaimGui(player)
                            }
                        }

                    if (auctionPage.currentPage < auctionPage.pageCount - 1) {
                        pane[5, 8] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.SPECTRAL_ARROW)
                                        .name(nextPageAction.get(player.language())),
                                ),
                            ) {
                                plugin.launch {
                                    player.inventory.close()
                                    openAuctionGui(player, auctionPage.currentPage + 1)
                                }
                            }
                    }
                }
            }

        auctionGui.open(player)
    }

    private suspend fun openCreateAuctionGui(player: Player) {
        if (player.inventory.itemInMainHand.type == Material.AIR) {
            player.sendMessage(holdItem.get(player.language()))
            return
        }

        val price = InterfaceProperty(configuredDefaultPrice)

        val createGui =
            buildChestInterface {
                rows = 6

                titleSupplier = {
                    createAuctionText.get(player.language())
                }

                withTransform(price) { pane, _ ->
                    forEachInGrid(6, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    pane[1, 4] =
                        StaticElement(
                            drawable(player.inventory.itemInMainHand.clone()),
                        )

                    pane[3, 2] =
                        StaticElement(
                            drawable(ItemStack(Material.REDSTONE).name(Component.text("-100", NamedTextColor.RED))),
                        ) {
                            price.value = maxOf(configuredMinimumPrice, price.value - 100L)
                        }

                    pane[3, 3] =
                        StaticElement(
                            drawable(ItemStack(Material.REDSTONE).name(Component.text("-10", NamedTextColor.RED))),
                        ) {
                            price.value = maxOf(configuredMinimumPrice, price.value - 10L)
                        }

                    pane[3, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.GOLD_INGOT)
                                    .name(
                                        auctionPrice.get(
                                            player.language(),
                                            listOf(price.value.toString()),
                                        ),
                                    ),
                            ),
                        )

                    pane[3, 5] =
                        StaticElement(
                            drawable(ItemStack(Material.LIME_DYE).name(Component.text("+10", NamedTextColor.GREEN))),
                        ) {
                            price.value += 10L
                        }

                    pane[3, 6] =
                        StaticElement(
                            drawable(ItemStack(Material.EMERALD).name(Component.text("+100", NamedTextColor.GREEN))),
                        ) {
                            price.value += 100L
                        }

                    pane[5, 3] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.BARRIER)
                                    .name(cancelAction.get(player.language())),
                            ),
                        ) {
                            player.inventory.close()
                        }

                    pane[5, 5] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.GREEN_WOOL)
                                    .name(createAuctionText.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                createAuction(player, price.value)
                            }
                        }
                }
            }

        createGui.open(player)
    }

    private suspend fun createAuction(
        player: Player,
        price: Long,
    ) {
        val profile = profileManager.getActiveProfile(player)

        if (profile == null) {
            player.sendMessage(noActiveProfile.get(player.language()))
            return
        }

        val item = player.inventory.itemInMainHand

        if (item.type == Material.AIR) {
            player.sendMessage(holdItem.get(player.language()))
            return
        }

        val auctionItem = item.clone()
        player.inventory.setItemInMainHand(ItemStack(Material.AIR))

        try {
            auctionManager.createAuction(
                sellerId = profile.id,
                itemStack = auctionItem.serializeForAuction(),
                price = price,
            )

            player.sendMessage(auctionCreated.get(player.language()))
            player.inventory.close()
            openAuctionGui(player)
        } catch (exception: Exception) {
            player.inventory.setItemInMainHand(auctionItem)
            player.sendMessage(auctionCreateFailed.get(player.language()))
        }
    }

    private suspend fun buyAuction(
        player: Player,
        auction: AuctionItem,
    ) {
        val activeProfile = profileManager.getActiveProfile(player)

        if (activeProfile == null) {
            player.sendMessage(noActiveProfile.get(player.language()))
            return
        }

        if (activeProfile.id == auction.sellerId) {
            player.sendMessage(ownAuction.get(player.language()))
            return
        }

        val buyerProfile = profileManager.getProfile(activeProfile.id)
        val sellerProfile = profileManager.getProfile(auction.sellerId)

        if (buyerProfile == null || sellerProfile == null) {
            player.sendMessage(profileNotFound.get(player.language()))
            return
        }

        val price = auction.price.toDouble()

        if (!profileManager.removeDollars(buyerProfile, price)) {
            player.sendMessage(notEnoughMoney.get(player.language()))
            return
        }

        if (!auctionManager.buyAuction(auction.id, buyerProfile.id)) {
            profileManager.addDollars(buyerProfile, price)
            player.sendMessage(auctionAlreadyBought.get(player.language()))
            return
        }

        profileManager.addDollars(sellerProfile, price)

        player.sendMessage(auctionBought.get(player.language()))
        player.inventory.close()
        openAuctionGui(player)
    }

    private suspend fun openClaimGui(
        player: Player,
        page: Int = 0,
    ) {
        val activeProfile = profileManager.getActiveProfile(player)

        if (activeProfile == null) {
            player.sendMessage(noActiveProfile.get(player.language()))
            return
        }

        val claimPage =
            paginate(
                auctionManager.getClaimableAuctions(activeProfile.id),
                page,
            )
        val claims = claimPage.entries

        val claimGui =
            buildChestInterface {
                rows = 6

                titleSupplier = {
                    claimTitle.get(player.language())
                }

                withTransform { pane, _ ->
                    forEachInGrid(6, 9) { row, column ->
                        pane[row, column] =
                            StaticElement(
                                drawable(createFiller(Material.GRAY_STAINED_GLASS_PANE)),
                            )
                    }

                    if (claims.isEmpty()) {
                        pane[2, 4] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.BARRIER)
                                        .name(noClaims.get(player.language())),
                                ),
                            )
                    }

                    claims.forEachIndexed { index, auction ->
                        val item =
                            runCatching {
                                auction.itemStack.deserializeAuctionItem()
                            }.getOrElse {
                                ItemStack(Material.BARRIER)
                            }

                        val itemMeta = item.itemMeta
                        val existingLore = itemMeta.lore() ?: emptyList()
                        val statusName =
                            if (auction.status == AuctionStatus.SOLD) {
                                boughtStatus.get(player.language())
                            } else {
                                expiredStatus.get(player.language())
                            }

                        itemMeta.lore(
                            existingLore +
                                listOf(
                                    Component.empty(),
                                    statusName,
                                    claimAction.get(player.language()),
                                ),
                        )

                        item.itemMeta = itemMeta

                        pane[index / 9, index % 9] =
                            StaticElement(
                                drawable(item),
                            ) {
                                plugin.launch {
                                    claimAuction(
                                        player,
                                        auction,
                                        claimPage.currentPage,
                                    )
                                }
                            }
                    }

                    if (claimPage.currentPage > 0) {
                        pane[5, 0] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.ARROW)
                                        .name(previousPageAction.get(player.language())),
                                ),
                            ) {
                                plugin.launch {
                                    player.inventory.close()
                                    openClaimGui(player, claimPage.currentPage - 1)
                                }
                            }
                    }

                    pane[5, 4] =
                        StaticElement(
                            drawable(
                                ItemStack(Material.PAPER)
                                    .name(backAction.get(player.language())),
                            ),
                        ) {
                            plugin.launch {
                                player.inventory.close()
                                openAuctionGui(player)
                            }
                        }

                    if (claimPage.currentPage < claimPage.pageCount - 1) {
                        pane[5, 8] =
                            StaticElement(
                                drawable(
                                    ItemStack(Material.SPECTRAL_ARROW)
                                        .name(nextPageAction.get(player.language())),
                                ),
                            ) {
                                plugin.launch {
                                    player.inventory.close()
                                    openClaimGui(player, claimPage.currentPage + 1)
                                }
                            }
                    }
                }
            }

        claimGui.open(player)
    }

    private suspend fun claimAuction(
        player: Player,
        auction: AuctionItem,
        page: Int,
    ) {
        val activeProfile = profileManager.getActiveProfile(player)

        if (activeProfile == null) {
            player.sendMessage(noActiveProfile.get(player.language()))
            return
        }

        val item =
            runCatching {
                auction.itemStack.deserializeAuctionItem()
            }.getOrElse {
                player.sendMessage(itemLoadFailed.get(player.language()))
                return
            }

        if (!hasInventorySpace(player, item)) {
            player.sendMessage(inventoryFull.get(player.language()))
            return
        }

        if (!auctionManager.claimAuction(auction.id, activeProfile.id)) {
            player.sendMessage(itemAlreadyClaimed.get(player.language()))
            return
        }

        player.inventory.addItem(item)
        player.sendMessage(itemClaimed.get(player.language()))
        player.inventory.close()
        openClaimGui(player, page)
    }

    private fun <T> paginate(
        entries: List<T>,
        requestedPage: Int,
    ): Page<T> {
        val pageCount =
            maxOf(
                1,
                (entries.size + auctionsPerPage - 1) / auctionsPerPage,
            )
        val currentPage = requestedPage.coerceIn(0, pageCount - 1)
        val pageEntries =
            entries
                .drop(currentPage * auctionsPerPage)
                .take(auctionsPerPage)

        return Page(
            entries = pageEntries,
            currentPage = currentPage,
            pageCount = pageCount,
        )
    }

    private fun hasInventorySpace(
        player: Player,
        item: ItemStack,
    ): Boolean {
        val simulatedInventory =
            Bukkit.createInventory(null, 36).apply {
                storageContents =
                    player.inventory.storageContents
                        .map { it?.clone() }
                        .toTypedArray()
            }

        return simulatedInventory.addItem(item.clone()).isEmpty()
    }

    private data class Page<T>(
        val entries: List<T>,
        val currentPage: Int,
        val pageCount: Int,
    )

    companion object {
        private const val MAX_AUCTIONS_PER_PAGE = 45
    }
}
