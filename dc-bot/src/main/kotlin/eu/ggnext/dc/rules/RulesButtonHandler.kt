package eu.ggnext.dc.rules

import eu.ggnext.dc.config.BotConfig
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class RulesButtonHandler(
    private val config: BotConfig,
) : ListenerAdapter() {
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == "rules:accept") {
            val member = event.member ?: return
            val guild = event.guild ?: return
            val verifiedRole = guild.getRoleById(config.VERIFIED_ID) ?: return

            if (member.roles.contains(verifiedRole)) {
                event.reply("You are already verified!").setEphemeral(true).queue()
                return
            }

            guild.addRoleToMember(member, verifiedRole).queue()
            event.reply("You are now verified!").setEphemeral(true).queue()
        }
    }
}
