package eu.ggnext.velocityCore.features.verify

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.velocityCore.features.players.PlayerManager
import eu.ggnext.velocityCore.utils.asPlayerOrNull
import eu.ggnext.velocityCore.utils.language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VerifyCommand(
    private val verifyManager: VerifyManager,
    private val playerManager: PlayerManager,
    private val scope: CoroutineScope,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("verify")
            .executes { ctx ->
                val player = ctx.source.asPlayerOrNull() ?: return@executes 1

                scope.launch {
                    if (playerManager.getPlayer(player.uniqueId)?.discordId != null) {
                        val msg by TranslationStore("translations.velocity.verification.already_verified")
                        player.sendMessage(msg.get(player.language()))
                    } else if (verifyManager.getActiveVerificationProcess(player.uniqueId) != null) {
                        val msg by TranslationStore("translations.velocity.verification.active_process")
                        player.sendMessage(
                            msg.get(
                                player.language(),
                                listOf(verifyManager.getActiveVerificationProcess(player.uniqueId)?.verifyCode.toString()),
                            ),
                        )
                    } else {
                        val code = verifyManager.createVerification(player.uniqueId)
                        val msg by TranslationStore("translations.velocity.verification.create_process")
                        player.sendMessage(msg.get(player.language(), listOf(code.toString())))
                    }
                }
                Command.SINGLE_SUCCESS
            }.build()
}
