package eu.ggnext.core.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ScoreBoardManager(
    private val scoreBoardLibrary: ScoreboardLibrary,
) {
    private val scoreboards = ConcurrentHashMap<UUID, Sidebar>()

    fun addPlayer(player: Player) {
        val scoreboard = scoreBoardLibrary.createSidebar()

        scoreboard.addPlayer(player)
        showDefaultScoreboard(scoreboard)

        scoreboards[player.uniqueId] = scoreboard
    }

    fun removePlayer(player: Player) {
        scoreboards[player.uniqueId]?.close()
        scoreboards.remove(player.uniqueId)
    }

    fun getScoreBoard(player: Player): Sidebar? = scoreboards[player.uniqueId]

    fun shutdown() {
        scoreboards.forEach { it.value.close() }
        scoreboards.clear()
    }

    fun showDefaultScoreboard(scoreboard: Sidebar) {
        val lines =
            SidebarComponent
                .builder()
                .addBlankLine()
                .addStaticLine { Component.text("play.ggnext.de", NamedTextColor.AQUA) }
                .build()

        ComponentSidebarLayout(
            SidebarComponent.staticLine(Component.text("GGNext", NamedTextColor.GREEN)),
            lines,
        ).apply(scoreboard)
    }
}
