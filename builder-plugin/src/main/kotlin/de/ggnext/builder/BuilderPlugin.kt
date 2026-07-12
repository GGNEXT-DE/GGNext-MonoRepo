package de.ggnext.builder

import de.ggnext.builder.commands.PDeleteCommand
import de.ggnext.builder.commands.PExportCommand
import de.ggnext.builder.commands.PSetCommand
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class BuilderPlugin : JavaPlugin() {
    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdir()
        registerCommands()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(PSetCommand(this).command)
            commands.register(PDeleteCommand(this).command)
            commands.register(PExportCommand(this).command)
        }
    }
}
