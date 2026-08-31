package eu.ggnext.dc.tickets

import dev.minn.jda.ktx.messages.EmbedBuilder
import eu.ggnext.contentsystem.value.store.StringStore
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.dc.config.BotConfig
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class TicketButtonHandler(
    private val config: BotConfig,
) : ListenerAdapter() {
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            "ticket:close" -> handleTicketClose(event)
        }
    }

    private fun handleTicketClose(event: ButtonInteractionEvent) {
        val channel = event.channel as ThreadChannel

        channel.manager.setLocked(true).queue()
        channel.manager.setArchived(true).queue()

        val ticketCreator = getTicketCreator(event) ?: return

        val ticketEmbedTitle by StringStore("strings.discord.ticket.close.title")
        val ticketEmbedDescription by StringStore("strings.discord.ticket.close.description")

        val embed =
            EmbedBuilder {
                title = ticketEmbedTitle
                description = "$ticketEmbedDescription | ${channel.asMention}"
            }
        ticketCreator.user.openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(embed.build()).queue()
        }

        val ticketLogChannel = event.guild?.getChannelById(TextChannel::class.java, config.TICKET_LOG_ID)
        ticketLogChannel?.sendMessageEmbeds(embed.build())?.queue()

        event.reply("Ticket was closed.").setEphemeral(true).queue()
    }

    private fun getTicketCreator(event: ButtonInteractionEvent): Member? {
        val memberId =
            event.channel.name
                .split("-")
                .last()
        return event.guild?.getMemberById(memberId)
    }
}
