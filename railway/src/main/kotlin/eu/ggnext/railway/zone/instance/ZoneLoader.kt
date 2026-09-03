package eu.ggnext.railway.zone.instance

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import eu.ggnext.contentsystem.value.store.NumberStore
import eu.ggnext.railway.zone.config.ZoneConfig
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin
import java.io.FileInputStream

class ZoneLoader(
    private val plugin: JavaPlugin,
) {
    private val world =
        Bukkit.getWorld("void_world")
            ?: WorldCreator("void_world").createWorld()
            ?: throw RuntimeException("Could not load void_world!")
    private val weWorld = BukkitAdapter.adapt(world)

    private val xDifference by NumberStore("numbers.railway.zone.x_difference")

    suspend fun spawnSlot(
        slot: Int,
        zone: ZoneConfig,
    ) {
        val xCoordinates = xDifference * slot
        val file = zone.schemFile

        withContext(plugin.asyncDispatcher) {
            val format = ClipboardFormats.findByFile(file) ?: return@withContext
            val clipboard = format.getReader(FileInputStream(file)).use { it.read() }

            WorldEdit
                .getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .limitUnlimited()
                .build()
                .use { editSession ->
                    val operation =
                        ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(xCoordinates.toInt(), 0, 0))
                            .ignoreAirBlocks(false)
                            .build()
                    Operations.complete(operation)
                }
        }
    }

    suspend fun cleanSlot(slot: Int) {
        val xCoordinates = xDifference * slot

        val min = BlockVector3.at(xCoordinates.toInt(), 0, 0)

        val max = BlockVector3.at(xCoordinates.toInt() + 250, 300, 300)

        val region: Region = CuboidRegion(weWorld, min, max)

        withContext(plugin.asyncDispatcher) {
            WorldEdit
                .getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .limitUnlimited()
                .build()
                .use { editSession ->
                    editSession.setBlocks(region, BlockTypes.AIR)
                }
        }
    }
}
