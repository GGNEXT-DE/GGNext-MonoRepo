package eu.ggnext.core.command

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandSendEvent

class CommandVisibilityFilter(
    private val prod: Boolean,
) : Listener {
    @EventHandler
    fun onPlayerCommandSend(event: PlayerCommandSendEvent) {
        if (prod && !event.player.hasPermission("group.team")) {
            val newCommandList =
                event.commands
                    .filter { it.startsWith("ggnextcore:") || it.startsWith("railway:") }
                    .toMutableList()

            newCommandList.toList().forEach { command ->
                newCommandList.add(command.substringAfter(":"))
            }

            event.commands.clear()
            event.commands.addAll(newCommandList)
        }
    }
}
