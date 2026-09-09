package eu.ggnext.dc.verify

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.player.Player
import eu.ggnext.common.verify.VerifyPlayer
import eu.ggnext.contentsystem.value.store.StringStore
import kotlinx.coroutines.flow.firstOrNull
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.jda.actor.SlashCommandActor

class VerifyCommand(
    private val mongoManager: MongoManager,
) {
    private val verifyCollection = mongoManager.database.getCollection<VerifyPlayer>("verify_requests")
    private val playerCollection = mongoManager.database.getCollection<Player>("players")

    @Command("verify")
    @Description("Connect your minecraft and discord account")
    suspend fun verify(
        actor: SlashCommandActor,
        code: Int,
    ) {
        val verifyRequest = verifyCollection.find(Filters.eq("verifyCode", code)).firstOrNull()

        if (verifyRequest == null) {
            val codeNotFound by StringStore("strings.discord.verify.code_not_found")
            actor.replyToInteraction(codeNotFound).setEphemeral(true).queue()
            return
        }

        if (verifyRequest.expiresAt < System.currentTimeMillis()) {
            val requestExpired by StringStore("strings.discord.verify.expired")
            actor.replyToInteraction(requestExpired).setEphemeral(true).queue()
            return
        }

        val playerDocument = playerCollection.find(Filters.eq("_id", verifyRequest.id)).firstOrNull()

        if (playerDocument?.discordId != null) {
            val alreadyConnected by StringStore("strings.discord.verify.already_connected")
            actor.replyToInteraction(alreadyConnected).setEphemeral(true).queue()
            return
        }

        playerCollection.updateOne(Filters.eq("_id", verifyRequest.id), Updates.set("discordId", actor.user().idLong))
        verifyCollection.deleteOne(Filters.eq("_id", verifyRequest.id))

        val connected by StringStore("strings.discord.verify.connected")
        actor.replyToInteraction(connected).setEphemeral(true).queue()
    }
}
