package eu.ggnext.railway.scoreboard

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.language
import eu.ggnext.railway.profile.RailwayProfileManager
import kotlinx.coroutines.Job
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class RailwayScoreBoardManager(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val coreSidebars: eu.ggnext.core.scoreboard.ScoreBoardManager,
) {
    private val playerRefreshTasks = ConcurrentHashMap<UUID, Long>()
    private val refreshIntervalTicks = 40L // 2 seconds (40 ticks = 2 seconds at 20 TPS)
    private val progressBarLength = 10
    private var autoRefreshJob: Job? = null
    private val isShuttingDown = AtomicBoolean(false)

    private val scoreboardTitle by TranslationStore("translations.railway.scoreboard.title")
    private val characterNameKey by TranslationStore("translations.railway.scoreboard.character_name")
    private val dollarsKey by TranslationStore("translations.railway.scoreboard.dollars")
    private val levelKey by TranslationStore("translations.railway.scoreboard.level")
    private val skillPointsKey by TranslationStore("translations.railway.scoreboard.skill_points")
    private val zoneKey by TranslationStore("translations.railway.scoreboard.current_zone")
    private val serverIpKey by TranslationStore("translations.railway.scoreboard.server_ip")
    private val ipLabelKey by TranslationStore("translations.railway.scoreboard.ip_label")

    suspend fun activateForPlayer(player: Player) {
        // Get or create sidebar through core scoreboard manager
        log.info("Activating Railway Scoreboard for ${player.name}")
        var sidebar = coreSidebars.getScoreBoard(player)
        if (sidebar == null) {
            // If no sidebar exists yet, add player to create one
            coreSidebars.addPlayer(player)
            sidebar = coreSidebars.getScoreBoard(player)
        }

        if (sidebar != null) {
            updateScoreboard(player)
            playerRefreshTasks[player.uniqueId] = System.currentTimeMillis()
            log.info("Railway Scoreboard activated for ${player.name}")
        } else {
            log.warn("Could not activate Railway Scoreboard for ${player.name}")
        }
    }

    fun deactivateForPlayer(player: Player) {
        // Remove player from core scoreboard manager
        coreSidebars.removePlayer(player)
        playerRefreshTasks.remove(player.uniqueId)
    }

    suspend fun updateScoreboard(player: Player) {
        val activeProfileId = profileManager.getActiveProfileId(player)
        if (activeProfileId == null) {
            log.warn("Railway Scoreboard: No active profile selected for ${player.name}")
            return
        }
        val profile =
            try {
                profileManager.getProfile(activeProfileId)
            } catch (e: Exception) {
                log.warn("Railway Scoreboard: Error loading profile for ${player.name}: ${e.message}")
                return
            }
        if (profile == null) {
            log.warn("Railway Scoreboard: No active profile selected for ${player.name}")
            return
        }
        val sidebar =
            try {
                coreSidebars.getScoreBoard(player)
            } catch (e: Exception) {
                log.warn("Railway Scoreboard: Error getting scoreboard for ${player.name}: ${e.message}")
                return
            }
        if (sidebar == null) {
            log.warn("Railway Scoreboard: Sidebar is null for ${player.name}")
            return
        }

        val characterName = profile.name
        val dollars = profile.railwayDollars
        val level = profile.level.currentLevel
        val currentXp = profile.level.xp
        val maxXp = (100L * level * level)
        val skillPoints = profile.level.skillPoints
        val zone = player.world.name // or custom zone tracking

        val lines = mutableListOf<Component>()

        // Profile Name
        lines.add(
            characterNameKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(Component.text(" $characterName", NamedTextColor.AQUA)),
        )

        lines.add(Component.empty())

        // Level and XP Progress Bar
        val xpPercent = if (maxXp > 0) ((currentXp * 100) / maxXp).toInt() else 0
        val xpProgressBar = createProgressBar(xpPercent)
        lines.add(
            Component.text("[$xpProgressBar] $xpPercent%", NamedTextColor.GRAY),
        )

        // Level
        lines.add(
            levelKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(Component.text(" $level", NamedTextColor.YELLOW)),
        )

        lines.add(Component.empty())

        // Dollars
        lines.add(
            dollarsKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(Component.text(" $dollars $", NamedTextColor.GOLD)),
        )

        // Skill Points
        lines.add(
            skillPointsKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(Component.text(" $skillPoints", NamedTextColor.LIGHT_PURPLE)),
        )

        // Current Zone
        lines.add(
            zoneKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(Component.text(" $zone", NamedTextColor.BLUE)),
        )

        lines.add(Component.empty())

        // Server IP
        lines.add(
            ipLabelKey
                .get(player.language())
                .color(NamedTextColor.GRAY)
                .append(
                    Component
                        .text(" ")
                        .append(
                            serverIpKey
                                .get(player.language())
                                .decorate(TextDecoration.BOLD)
                                .color(NamedTextColor.GREEN),
                        ),
                ),
        )

        // Create sidebar layout with Railway title and profile stats
        val sidebarComponent =
            SidebarComponent
                .builder()
                .apply {
                    addBlankLine()
                    lines.forEach { addStaticLine { it } }
                }.build()

        val title =
            scoreboardTitle
                .get(player.language())
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.AQUA)

        ComponentSidebarLayout(
            SidebarComponent.staticLine(title),
            sidebarComponent,
        ).apply(sidebar)
    }

    suspend fun startAutoRefresh() {
        isShuttingDown.set(false)
        autoRefreshJob =
            plugin.launch {
                while (!isShuttingDown.get()) {
                    try {
                        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
                            val lastUpdate = playerRefreshTasks[player.uniqueId] ?: return@forEach
                            val now = System.currentTimeMillis()

                            if (now - lastUpdate >= (refreshIntervalTicks * 50)) { // Convert ticks to ms
                                updateScoreboard(player)
                                playerRefreshTasks[player.uniqueId] = now
                            }
                        }
                    } catch (e: Exception) {
                        if (!isShuttingDown.get()) {
                            log.warn("Railway Scoreboard auto-refresh error: ${e.message}")
                        }
                    }
                    kotlinx.coroutines.delay((refreshIntervalTicks * 50).milliseconds)
                }
            }
    }

    fun shutdown() {
        isShuttingDown.set(true)
        autoRefreshJob?.cancel()
        playerRefreshTasks.clear()
    }

    private fun createProgressBar(percent: Int): String {
        val filled = (percent * progressBarLength / 100).coerceIn(0, progressBarLength)
        val empty = progressBarLength - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
}
