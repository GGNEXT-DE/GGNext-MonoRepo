package eu.ggnext.dc.verify

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.logging.info
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
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
        val discordId = actor.user().idLong
        val verifyRequest = verifyCollection.find(Filters.eq("verifyCode", code)).firstOrNull()

        if (verifyRequest == null) {
            log.warn("Discord user $discordId tried to verify with unknown code $code")
            val codeNotFound by StringStore("strings.discord.verify.code_not_found")
            actor.replyToInteraction(codeNotFound).setEphemeral(true).queue()
            return
        }

        if (verifyRequest.expiresAt < System.currentTimeMillis()) {
            log.warn("Discord user $discordId tried to verify with expired code for player ${verifyRequest.id}")
            val requestExpired by StringStore("strings.discord.verify.expired")
            actor.replyToInteraction(requestExpired).setEphemeral(true).queue()
            return
        }

        val playerDocument = playerCollection.find(Filters.eq("_id", verifyRequest.id)).firstOrNull()

        if (playerDocument == null) {
            log.warn("Discord user $discordId tried to verify but player ${verifyRequest.id} does not exist")
            val playerNotFound by StringStore("strings.discord.verify.player_not_found")
            actor.replyToInteraction(playerNotFound).setEphemeral(true).queue()
            return
        }

        if (playerDocument.discordId != null) {
            log.warn(
                "Discord user $discordId tried to verify player ${verifyRequest.id} which is already connected to discord user ${playerDocument.discordId}",
            )
            val alreadyConnected by StringStore("strings.discord.verify.already_connected")
            actor.replyToInteraction(alreadyConnected).setEphemeral(true).queue()
            return
        }

        val updateResult =
            playerCollection.updateOne(
                Filters.eq("_id", verifyRequest.id),
                Updates.set("discordId", discordId),
            )

        if (updateResult.modifiedCount > 0) {
            verifyCollection.deleteOne(Filters.eq("_id", verifyRequest.id))

            log.info("Discord user $discordId verified player ${verifyRequest.id}")
            val connected by StringStore("strings.discord.verify.connected")
            actor.replyToInteraction(connected).setEphemeral(true).queue()
        } else {
            log.warn("Failed to verify player ${verifyRequest.id} for discord user $discordId: update did not modify any document")
            val internalError by StringStore("strings.discord.verify.internal_error")
            actor.replyToInteraction(internalError).setEphemeral(true).queue()
        }
    }
}
