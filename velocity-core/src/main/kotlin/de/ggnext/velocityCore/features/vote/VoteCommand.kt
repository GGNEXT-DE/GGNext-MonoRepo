package de.ggnext.velocityCore.features.vote

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.contentsystem.value.store.StringStore
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

class VoteCommand {
    private val configuredVoteUrl by StringStore("strings.velocity.vote.url")

    val command =
        BrigadierCommand
            .literalArgumentBuilder("vote")
            .executes { ctx ->
                val player = ctx.source.asPlayerOrNull() ?: return@executes 1
                val voteUrl = configuredVoteUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: DEFAULT_VOTE_URL

                val message by TranslationStore("translations.velocity.vote.link")
                player.sendMessage(
                    message
                        .get(player.language(), listOf(voteUrl))
                        .clickEvent(ClickEvent.openUrl(voteUrl))
                        .hoverEvent(HoverEvent.showText(message.get(player.language(), listOf(voteUrl)))),
                )

                Command.SINGLE_SUCCESS
            }.build()

    private companion object {
        const val DEFAULT_VOTE_URL = "https://minecraft-server.eu"
    }
}
