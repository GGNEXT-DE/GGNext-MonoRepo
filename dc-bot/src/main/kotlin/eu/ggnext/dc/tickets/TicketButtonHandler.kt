package eu.ggnext.dc.tickets

import dev.minn.jda.ktx.messages.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class TicketButtonHandler : ListenerAdapter() {
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

        val embed =
            EmbedBuilder {
                title = "Ticket Closed"
                description = "Your Ticket has been closed | ${channel.asMention}"
            }
        ticketCreator.user.openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(embed.build()).queue()
        }
    }

    private fun getTicketCreator(event: ButtonInteractionEvent): Member? {
        val memberId =
            event.channel.name
                .split("-")
                .last()
        return event.guild?.getMemberById(memberId)
    }
}
