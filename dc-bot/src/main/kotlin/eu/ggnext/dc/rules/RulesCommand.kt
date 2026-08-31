package eu.ggnext.dc.rules

import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import revxrsal.commands.annotation.Command
import revxrsal.commands.jda.actor.SlashCommandActor
import revxrsal.commands.jda.annotation.CommandPermission

class RulesCommand {
    @Command("send-rules")
    @CommandPermission(Permission.ADMINISTRATOR)
    fun sendRules(actor: SlashCommandActor) {
        val channel = actor.channel()

        val acceptButton = Button.of(ButtonStyle.SUCCESS, "rules:accept", "Accept the rules")

        channel
            .sendMessageEmbeds(getRulesEmbed().build())
            .addComponents(ActionRow.of(acceptButton))
            .queue()
        actor.replyToInteraction("Embed send").setEphemeral(true).queue()
    }

    private fun getRulesEmbed(): InlineEmbed =
        EmbedBuilder {
            title = "Welcome to GGNext Rules"
            description = "# Welcome to the official GGNEXT Discord Server!\n" +
                "\n" +
                "To gain access to all channels and the Minecraft server, you must accept our rules of conduct.\n" +
                "\n" +
                "\uD83D\uDCDC **Rules of Conduct**\n" +
                "\n" +
                "**Respectful Behavior:** Treat other users the way you would like to be treated. Insults, provocations, " +
                "or discrimination of any kind will not be tolerated.\n" +
                "\n" +
                "**No Spam:** Do not spam text or voice channels. This includes excessive use of caps, emojis, or mentions.\n" +
                "\n" +
                "**No Self-Promotion:** Advertising other Minecraft servers, Discord servers, or products " +
                "is prohibited without explicit permission.\n" +
                "\n" +
                "**Content:** Sharing illegal, racist, pornographic, or glorifying-violence content is strictly prohibited.\n" +
                "\n" +
                "**Privacy:** Respect the privacy of others. Do not share private or personal information (doxxing).\n" +
                "\n" +
                "\uD83D\uDEE0\uFE0F **Administration & Punishments**\n" +
                "\n" +
                "The GGNEXT team reserves the right to take disciplinary action against users at its own discretion," +
                " even if no direct violation of these written rules has occurred, if the behavior negatively affects" +
                " the server environment.\n" +
                "\n" +
                "\uD83D\uDD17 **Discord Guidelines**\n" +
                "\n" +
                "In addition to our rules, Discord's official Community Guidelines apply. You can read them here:\n" +
                "\n" +
                "\uD83D\uDC49 https://discord.com/guidelines\n" +
                "\n" +
                "Click the button below to accept the rules and verify yourself.\n"
        }
}
