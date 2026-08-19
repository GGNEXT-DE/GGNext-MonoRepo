package de.ggnext.protocol

const val PROTOCOL_VERSION = 1

object Routes {
    const val FRIEND_ADD = "friend.add"
    const val FRIEND_ACCEPT = "friend.accept"
    const val FRIEND_DENY = "friend.deny"
    const val FRIEND_REMOVE = "friend.remove"
    const val FRIEND_LIST = "friend.list"

    const val PARTY_INVITE = "party.invite"
    const val PARTY_ACCEPT = "party.accept"
    const val PARTY_DENY = "party.deny"
    const val PARTY_KICK = "party.kick"
    const val PARTY_LEAVE = "party.leave"
    const val PARTY_DISBAND = "party.disband"
    const val PARTY_MEMBERS = "party.members"

    const val VERIFY_CREATE = "verify.create"
    const val VERIFY_COMPLETE = "verify.complete"
    const val VERIFY_GET = "verify.get"

    const val PRESENCE_UPDATE = "presence.update"
    const val PRESENCE_ONLINE = "presence.online"

    const val CONTENT_SNAPSHOT = "content.snapshot"
    const val CONTENT_ENSURE_DEFAULT = "content.ensureDefault"

    const val ECONOMY_GET = "economy.get"
    const val ECONOMY_ADD = "economy.add"
    const val ECONOMY_REMOVE = "economy.remove"

    const val PLAYER_GET = "player.get"
    const val PLAYER_GET_BY_NAME = "player.getByName"
    const val PLAYER_LOGIN = "player.login"
    const val PLAYER_SAVE_PLAYTIME = "player.savePlaytime"

    const val PUNISH_BAN = "punish.ban"
    const val PUNISH_TEMP_BAN = "punish.tempBan"
    const val PUNISH_MUTE = "punish.mute"
    const val PUNISH_TEMP_MUTE = "punish.tempMute"
    const val PUNISH_WARN = "punish.warn"
    const val PUNISH_UNBAN = "punish.unban"
    const val PUNISH_UNMUTE = "punish.unmute"
    const val PUNISH_HISTORY = "punish.history"
    const val PUNISH_ACTIVE_BAN = "punish.activeBan"
    const val PUNISH_ACTIVE_MUTE = "punish.activeMute"
}

object Channels {
    const val RPC_REQUEST_STREAM = "ggnext:rpc:req"
    const val RPC_CONSUMER_GROUP = "backend"

    const val EVENT_FRIEND = "ggnext:event:friend"
    const val EVENT_PARTY = "ggnext:event:party"
    const val EVENT_PRESENCE = "ggnext:event:presence"
    const val EVENT_CONTENT = "ggnext:event:content"

    fun replyChannel(instanceId: String) = "ggnext:rpc:reply:$instanceId"
}
