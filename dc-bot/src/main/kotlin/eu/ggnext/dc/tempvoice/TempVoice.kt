package eu.ggnext.dc.tempvoice

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class TempVoice(
    val channelId: String,
    val suffix: String,
) : ListenerAdapter() {
    private val cache = mutableListOf<String>()

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val joined = event.channelJoined
        val left = event.channelLeft
        val member = event.member

        if (joined != null && joined.id == channelId && member.voiceState?.channel != null) {
            handleJoined(joined.asVoiceChannel(), member)
            return
        }

        if (left != null && cache.contains(left.id) && left.asVoiceChannel().members.isEmpty()) {
            handleLeft(left.asVoiceChannel())
            return
        }
    }

    fun handleShutdown(guild: Guild?) {
        cache.forEach {
            guild?.getVoiceChannelById(it)?.delete()?.queue()
        }
    }

    private fun handleJoined(
        channel: VoiceChannel,
        member: Member,
    ) {
        val category = channel.parentCategory ?: return
        val guild = member.guild

        category
            .createVoiceChannel("${member.user.effectiveName}'s | $suffix")
            .queue { channel ->
                guild.moveVoiceMember(member, channel).queue()
                cache.add(channel.id)
            }
    }

    private fun handleLeft(channel: VoiceChannel) {
        cache.remove(channel.id)
        channel.delete().queue()
    }
}
