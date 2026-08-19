package de.ggnext.velocityCore.features.vote

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.proxy.ProxyServer
import com.vexsoftware.votifier.velocity.event.VotifierEvent
import de.ggnext.contentsystem.value.store.NumberStore
import de.ggnext.contentsystem.value.store.TranslationStore
import de.ggnext.sdk.feature.EconomySdk
import de.ggnext.sdk.feature.PlayersSdk
import de.ggnext.velocityCore.utils.language
import kotlin.jvm.optionals.getOrNull

class VoteRewardListener(
    private val server: ProxyServer,
    private val players: PlayersSdk,
    private val economy: EconomySdk,
) {
    private val rewardGems by NumberStore("numbers.velocity.vote.reward_gems")

    @Subscribe
    suspend fun onVote(event: VotifierEvent) {
        val vote = event.vote

        if (!vote.serviceName.contains("minecraft-server.eu", ignoreCase = true)) return

        val player = players.getByName(vote.username) ?: return

        if (!economy.addGems(player.id, rewardGems)) return

        server.getPlayer(player.id).getOrNull()?.let { onlinePlayer ->
            val message by TranslationStore("translations.velocity.vote.rewarded")
            onlinePlayer.sendMessage(message.get(onlinePlayer.language(), listOf(rewardGems.toString())))
        }
    }
}
