package eu.ggnext.builder.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.regions.Region
import eu.ggnext.common.types.ConfigPositionType
import eu.ggnext.common.types.MarkerData
import io.papermc.paper.command.brigadier.Commands
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class PExportCommand(
    private val plugin: JavaPlugin,
) {
    val command =
        Commands
            .literal("position-export")
            .requires { it.sender is Player && it.sender.hasPermission("ggnext.position.export") }
            .then(
                Commands
                    .argument("name", StringArgumentType.string())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val region = getPlayerSelection(player)
                        val name = StringArgumentType.getString(ctx, "name")

                        if (region == null) {
                            player.sendMessage(
                                Component.text("No region selected!", NamedTextColor.RED),
                            )
                            return@executes 0
                        }

                        val exportFolder =
                            plugin.dataFolder
                                .resolve("export")
                                .resolve(name)

                        exportFolder.mkdirs()

                        createJson(
                            region,
                            player,
                            exportFolder.resolve("markers.json"),
                        )

                        createSchematic(
                            region,
                            player,
                            exportFolder.resolve("build.schem"),
                        )

                        player.sendMessage(
                            Component.text(
                                "Exported '$name' successfully.",
                                NamedTextColor.GREEN,
                            ),
                        )

                        Command.SINGLE_SUCCESS
                    },
            ).build()

    private fun getPlayerSelection(player: Player): Region? {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)

        return try {
            session.getSelection(BukkitAdapter.adapt(player.world))
        } catch (_: IncompleteRegionException) {
            null
        }
    }

    private fun createSchematic(
        region: Region,
        player: Player,
        outputFile: File,
    ) {
        val world = BukkitAdapter.adapt(player.world)

        val clipboard = BlockArrayClipboard(region)
        clipboard.origin = region.minimumPoint

        WorldEdit.getInstance().newEditSession(world).use { editSession ->
            val copy =
                ForwardExtentCopy(
                    editSession,
                    region,
                    clipboard,
                    region.minimumPoint,
                )

            copy.isCopyingEntities = false
            Operations.complete(copy)
        }

        outputFile.outputStream().use { output ->
            BuiltInClipboardFormat.FAST_V3.getWriter(output).use { writer ->
                writer.write(clipboard)
            }
        }
    }

    private fun createJson(
        region: Region,
        player: Player,
        outputFile: File,
    ) {
        val markers = mutableListOf<MarkerData>()

        player.world.entities
            .filterIsInstance<TextDisplay>()
            .filter {
                region.contains(
                    BukkitAdapter.asBlockVector(it.location),
                )
            }.forEach { textDisplay ->

                val typeString =
                    textDisplay.persistentDataContainer.get(
                        NamespacedKey(plugin, "type"),
                        PersistentDataType.STRING,
                    ) ?: return@forEach

                val type =
                    runCatching {
                        ConfigPositionType.valueOf(typeString)
                    }.getOrNull() ?: return@forEach

                val origin = region.minimumPoint

                markers +=
                    MarkerData(
                        type = type,
                        x = textDisplay.location.x - origin.x(),
                        y = textDisplay.location.y - origin.y(),
                        z = textDisplay.location.z - origin.z(),
                    )
            }

        outputFile.writeText(
            Json { prettyPrint = true }
                .encodeToString(markers),
        )
    }
}
