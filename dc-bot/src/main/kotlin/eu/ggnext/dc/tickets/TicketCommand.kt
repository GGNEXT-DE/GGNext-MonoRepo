package eu.ggnext.dc.tickets

import dev.minn.jda.ktx.messages.EmbedBuilder
import eu.ggnext.contentsystem.value.store.StringStore
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import revxrsal.commands.annotation.Command
import revxrsal.commands.jda.actor.SlashCommandActor
import revxrsal.commands.jda.annotation.CommandPermission

class TicketCommand {
    @Command("send-ticket")
    @CommandPermission(Permission.ADMINISTRATOR)
    fun sendTicket(actor: SlashCommandActor) {
        val channel = actor.channel()

        val ticketEmbedTitle by StringStore("strings.discord.ticket.title")
        val ticketEmbedDescription by StringStore("strings.discord.ticket.description")

        val embed =
            EmbedBuilder {
                title = ticketEmbedTitle
                description = ticketEmbedDescription
            }.build()
        channel.sendMessageEmbeds(embed).addComponents(ActionRow.of(getSelectMenu())).queue()

        actor.replyToInteraction("Sent").setEphemeral(true).queue()
    }

    private fun getSelectMenu(): StringSelectMenu {
        val helpDescription by StringStore("strings.discord.help.description")
        val applicationDescription by StringStore("strings.discord.application.description")

        return StringSelectMenu
            .create("ticket:select")
            .addOption("Help", "support", helpDescription)
            .addOption("Application", "application", applicationDescription)
            .build()
    }
}
