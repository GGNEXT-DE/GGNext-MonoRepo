package eu.ggnext.dc.tickets

import dev.minn.jda.ktx.messages.EmbedBuilder
import eu.ggnext.dc.config.BotConfig
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class TicketDropDownHandler(
    private val config: BotConfig,
) : ListenerAdapter() {
    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        when (event.componentId) {
            "ticket:select" -> handleTicketSelect(event)
            else -> return
        }
    }

    private fun handleTicketSelect(event: StringSelectInteractionEvent) {
        val ticketTypeId = event.values.firstOrNull() ?: return
        val ticketType = TicketType.entries.firstOrNull { it.id == ticketTypeId } ?: return

        val channel = event.channel.asTextChannel()
        val member = event.member ?: return

        if (checkExisting(member, channel)) {
            event.reply("You already have a ticket!").setEphemeral(true).queue()
            return
        }

        setupChannel(ticketType, channel, member, event.guild)
    }

    private fun checkExisting(
        member: Member,
        channel: TextChannel,
    ): Boolean = channel.threadChannels.any { !it.isArchived && it.name.endsWith("-${member.id}") }

    private fun setupChannel(
        ticketType: TicketType,
        channel: TextChannel,
        member: Member,
        guild: Guild?,
    ) = channel.createThreadChannel("${ticketType.id}-${member.id}", true).queue { channel ->
        channel.addThreadMember(member).queue()
        channel.manager.setInvitable(false).queue()

        val supporterRole = guild?.getRoleById(config.SUPPORT_ID)
        val managerRole = guild?.getRoleById(config.MANAGER_ID)

        if (supporterRole != null && managerRole != null) {
            when (ticketType) {
                TicketType.APPLICATION -> {
                    channel.sendMessage(managerRole.asMention).queue()
                }
                TicketType.SUPPORT -> {
                    channel.sendMessage("${managerRole.asMention} | ${supporterRole.asMention}").queue()
                }
            }
        }


        val embed =
            EmbedBuilder {
                title = "Ticket ${ticketType.id}"
                description = ticketType.toString()
            }

        val closeButton = Button.of(ButtonStyle.DANGER, "ticket:close", "Close Ticket")

        channel.sendMessageEmbeds(embed.build()).addComponents(ActionRow.of(closeButton)).queue()
    }
}
