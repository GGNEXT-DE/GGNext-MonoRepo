package eu.ggnext.dc

import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.scope
import eu.ggnext.common.db.MongoManager
import eu.ggnext.contentsystem.ContentSystem
import eu.ggnext.dc.config.BotConfig
import eu.ggnext.dc.config.ConfigManager
import eu.ggnext.dc.rules.RulesButtonHandler
import eu.ggnext.dc.rules.RulesCommand
import eu.ggnext.dc.tempvoice.TempVoice
import eu.ggnext.dc.tickets.TicketButtonHandler
import eu.ggnext.dc.tickets.TicketCommand
import eu.ggnext.dc.tickets.TicketDropDownHandler
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import revxrsal.commands.jda.JDALamp
import revxrsal.commands.jda.JDAVisitors.slashCommands
import revxrsal.commands.jda.actor.SlashCommandActor

fun main() {
    Main()
}

class Main {
    companion object {
        lateinit var jda: JDA
        lateinit var config: BotConfig
    }

    init {
        config = ConfigManager.loadConfig()

        jda =
            default(config.TOKEN) {
                setActivity(Activity.playing("ggnext.eu"))
                intents +=
                    listOf(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES,
                    )
                intents -= listOf(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_TYPING)
                setMemberCachePolicy(MemberCachePolicy.ALL)
            }
        jda.awaitReady()

        val mongoManager = MongoManager(config.DATABASE_URI, config.DATABASE_NAME)

        jda.scope.launch {
            ContentSystem(mongoManager.database, jda.scope).also { it.init() }
        }

        val lamp = JDALamp.builder<SlashCommandActor>().build()
        lamp.register(TicketCommand())
        lamp.register(RulesCommand())
        lamp.accept(slashCommands(jda))

        jda.addEventListener(TicketDropDownHandler(config))
        jda.addEventListener(TicketButtonHandler(config))

        jda.addEventListener(RulesButtonHandler(config))

        val teamTempVoice = TempVoice(config.TEAM_TEMP_ID, "Team-Channel")
        jda.addEventListener(teamTempVoice)
        val sosTempVoice = TempVoice(config.SOS_TEMP_ID, "Sos-Channel")
        jda.addEventListener(sosTempVoice)
        val mainTempVoice = TempVoice(config.MAIN_TEMP_ID, "Channel")
        jda.addEventListener(mainTempVoice)

        Runtime.getRuntime().addShutdownHook(
            Thread {
                val guild = jda.getGuildById(config.GUILD_ID) ?: return@Thread
                teamTempVoice.handleShutdown(guild)
                mainTempVoice.handleShutdown(guild)
                sosTempVoice.handleShutdown(guild)
            },
        )
        jda.awaitShutdown()
    }
}
