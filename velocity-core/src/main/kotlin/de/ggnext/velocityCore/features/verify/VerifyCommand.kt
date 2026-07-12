package de.ggnext.velocityCore.features.verify

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.velocityCore.features.players.PlayerManager
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component

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
                        val msg by TranslationStore("translations.velocity.verification.alreadyVerified")
                        player.sendMessage(msg.get(player.language()))
                    } else if (verifyManager.getActiveVerificationProcess(player.uniqueId) != null) {
                        val msg by TranslationStore("translations.velocity.verification.activeProcess")
                        player.sendMessage(
                            msg.get(
                                player.language(),
                                listOf(verifyManager.getActiveVerificationProcess(player.uniqueId)?.verifyCode.toString()),
                            ),
                        )
                    } else {
                        val code = verifyManager.createVerification(player.uniqueId)
                        val msg by TranslationStore("translations.velocity.verification.createProcess")
                        player.sendMessage(msg.get(player.language(), listOf(code.toString())))
                    }
                }
                Command.SINGLE_SUCCESS
            }.build()
}
