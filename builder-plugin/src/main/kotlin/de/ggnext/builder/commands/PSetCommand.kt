package de.ggnext.builder.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import de.ggnext.common.types.ConfigPositionType
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class PSetCommand(
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("position-set")
            .requires { it.sender is Player && it.sender.hasPermission("ggnext.position.set") }
            .then(
                Commands
                    .argument("type", StringArgumentType.word())
                    .suggests { _, builder ->
                        ConfigPositionType.entries.forEach { builder.suggest(it.name.lowercase()) }
                        builder.buildFuture()
                    }.executes { ctx ->
                        val player = ctx.source.sender as Player
                        val location = player.location

                        val input = StringArgumentType.getString(ctx, "type")
                        val type = runCatching { ConfigPositionType.valueOf(input.uppercase()) }.getOrNull()
                        if (type == null) {
                            player.sendMessage(Component.text("Unknown config position type!", NamedTextColor.RED))
                            return@executes 0
                        }

                        spawnText(type, location)

                        Command.SINGLE_SUCCESS
                    },
            ).build()

    private fun spawnText(
        type: ConfigPositionType,
        location: Location,
    ) {
        val textDisplay = location.world.spawn(location.add(0.0, 1.0, 0.0), TextDisplay::class.java)

        textDisplay.persistentDataContainer.set(NamespacedKey(plugin, "type"), PersistentDataType.STRING, type.name)
        textDisplay.isPersistent = true
        textDisplay.billboard = Display.Billboard.CENTER

        val text: Component =
            MiniMessage.miniMessage().deserialize(
                "<gray><bold>Config Location</bold>\n\n<gray>type: <green>${type.name}</green>",
            )

        textDisplay.text(text)
    }
}
