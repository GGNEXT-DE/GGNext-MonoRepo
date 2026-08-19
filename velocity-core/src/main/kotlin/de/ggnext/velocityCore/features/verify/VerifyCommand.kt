package de.ggnext.velocityCore.features.verify

import com.mojang.brigadier.Command
import com.velocitypowered.api.command.BrigadierCommand
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.sdk.feature.PlayersSdk
import de.ggnext.sdk.feature.VerifySdk
import de.ggnext.velocityCore.utils.asPlayerOrNull
import de.ggnext.velocityCore.utils.language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VerifyCommand(
    private val verify: VerifySdk,
    private val players: PlayersSdk,
    private val scope: CoroutineScope,
) {
    val command =
        BrigadierCommand
            .literalArgumentBuilder("verify")
            .executes { ctx ->
                val player = ctx.source.asPlayerOrNull() ?: return@executes 1

                scope.launch {
                    val active = verify.getActive(player.uniqueId)
                    if (players.get(player.uniqueId)?.discordId != null) {
                        val msg by TranslationStore("translations.velocity.verification.alreadyVerified")
                        player.sendMessage(msg.get(player.language()))
                    } else if (active != null) {
                        val msg by TranslationStore("translations.velocity.verification.activeProcess")
                        player.sendMessage(msg.get(player.language(), listOf(active.code.toString())))
                    } else {
                        val code = verify.create(player.uniqueId).code
                        val msg by TranslationStore("translations.velocity.verification.createProcess")
                        player.sendMessage(msg.get(player.language(), listOf(code.toString())))
                    }
                }
                Command.SINGLE_SUCCESS
            }.build()
}
