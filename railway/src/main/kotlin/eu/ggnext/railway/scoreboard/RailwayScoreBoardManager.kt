package eu.ggnext.railway.scoreboard

import com.github.shynixn.mccoroutine.bukkit.launch
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.core.utils.language
import eu.ggnext.railway.profile.RailwayProfileManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RailwayScoreBoardManager(
    private val plugin: JavaPlugin,
    private val profileManager: RailwayProfileManager,
    private val coreSidebars: eu.ggnext.core.scoreboard.ScoreBoardManager,
) {
    private val playerRefreshTasks = ConcurrentHashMap<UUID, Long>()
    private val refreshIntervalTicks = 40L // 2 seconds (40 ticks = 2 seconds at 20 TPS)

    private val scoreboardTitle by TranslationStore("translations.railway.scoreboard.title")
    private val characterNameKey by TranslationStore("translations.railway.scoreboard.character_name")
    private val dollarsKey by TranslationStore("translations.railway.scoreboard.dollars")
    private val levelKey by TranslationStore("translations.railway.scoreboard.level")
    private val skillPointsKey by TranslationStore("translations.railway.scoreboard.skill_points")
    private val zoneKey by TranslationStore("translations.railway.scoreboard.current_zone")
    private val serverIpKey by TranslationStore("translations.railway.scoreboard.server_ip")

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
        val profile = profileManager.getActiveProfile(player)
        if (profile == null) {
            log.warn("Railway Scoreboard: No active profile selected for ${player.name}")
            return
        }
        val sidebar = coreSidebars.getScoreBoard(player)
        if (sidebar == null) {
            log.warn("Railway Scoreboard: Sidebar is null for ${player.name}")
            return
        }

        val characterName = profile.name
        val dollars = profile.railwayDollars
        val level = profile.level.currentLevel
        val currentXp = profile.level.xp.toLong()
        val maxXp = (100L * level * level)
        val skillPoints = profile.level.skillPoints
        val zone = player.world.name // or custom zone tracking

        val lines = mutableListOf<Component>()

        // Character Name
        lines.add(Component.empty())
        lines.add(
            characterNameKey
                .get(player.language())
                .append(Component.text(" $characterName", NamedTextColor.AQUA)),
        )

        // Dollars
        lines.add(
            dollarsKey
                .get(player.language())
                .append(Component.text(" $ $dollars", NamedTextColor.GOLD)),
        )

        // Level and XP Progress
        val xpPercent = if (maxXp > 0) ((currentXp * 100) / maxXp).toInt() else 0
        val xpProgressBar = createProgressBar(xpPercent, 10)
        lines.add(
            levelKey
                .get(player.language())
                .append(Component.text(" $level ", NamedTextColor.YELLOW))
                .append(Component.text("[$xpProgressBar]", NamedTextColor.GRAY))
                .append(Component.text(" $currentXp/$maxXp", NamedTextColor.DARK_GRAY)),
        )

        // Skill Points
        lines.add(
            skillPointsKey
                .get(player.language())
                .append(Component.text(" $skillPoints", NamedTextColor.LIGHT_PURPLE)),
        )

        // Current Zone
        lines.add(
            zoneKey
                .get(player.language())
                .append(Component.text(" $zone", NamedTextColor.BLUE)),
        )

        lines.add(Component.empty())

        // Server IP
        lines.add(
            serverIpKey
                .get(player.language())
                .decorate(TextDecoration.BOLD)
                .color(NamedTextColor.GREEN),
        )

        // Create sidebar layout with Railway title and profile stats
        val sidebarComponent =
            SidebarComponent
                .builder()
                .apply {
                    addBlankLine()
                    lines.forEach { addStaticLine { it } }
                    addBlankLine()
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
        log.info("Railway Scoreboard updated for ${player.name}: $characterName | $dollars$ | Level $level | $skillPoints SP")
    }

    suspend fun startAutoRefresh() {
        plugin.launch {
            while (true) {
                org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
                    val lastUpdate = playerRefreshTasks[player.uniqueId] ?: return@forEach
                    val now = System.currentTimeMillis()

                    if (now - lastUpdate >= (refreshIntervalTicks * 50)) { // Convert ticks to ms
                        updateScoreboard(player)
                        playerRefreshTasks[player.uniqueId] = now
                    }
                }
                kotlinx.coroutines.delay((refreshIntervalTicks * 50).toLong())
            }
        }
    }

    fun shutdown() {
        playerRefreshTasks.clear()
    }

    private fun createProgressBar(
        percent: Int,
        length: Int,
    ): String {
        val filled = (percent * length / 100).coerceIn(0, length)
        val empty = length - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
}
