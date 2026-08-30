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

        val embed =
            EmbedBuilder {
                title = "Ticket"
            }.build()
        channel.sendMessageEmbeds(embed).addComponents(ActionRow.of(getSelectMenu())).queue()

        actor.replyToInteraction("Sent").setEphemeral(true).queue()
    }

    private fun getSelectMenu(): StringSelectMenu {
        val helpDescription by StringStore("strings.discord.help.description")
        val bugReportDescription by StringStore("strings.discord.bug.report.description")
        val userReportDescription by StringStore("strings.discord.user.report.description")
        val applicationDescription by StringStore("strings.discord.application.description")

        return StringSelectMenu
            .create("ticket:select")
            .addOption("Help", "support", helpDescription)
            .addOption("Bug-Report", "bug-report", bugReportDescription)
            .addOption("User-Report", "user-report", userReportDescription)
            .addOption("Application", "application", applicationDescription)
            .build()
    }
}
