package de.ggnext.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.scope
import com.noxcrew.interfaces.InterfacesListeners
import de.ggnext.contentsystem.ContentSystem
import de.ggnext.core.api.GGNextAPI
import de.ggnext.core.db.MongoManager
import de.ggnext.core.economy.EconomyService
import de.ggnext.core.vanish.VanishCommand
import de.ggnext.core.vanish.VanishManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

class GGNextCore : SuspendingJavaPlugin() {
    private lateinit var mongoManager: MongoManager
    private lateinit var economyService: EconomyService
    private lateinit var contentSystem: ContentSystem

    override suspend fun onEnableAsync() {
        saveDefaultConfig()

        InterfacesListeners.install(this)

        mongoManager = MongoManager(config)
        GGNextAPI.mongoManager = mongoManager

        registerCommands()
        contentSystem = ContentSystem(mongoManager.database, scope).also { it.init() }

        economyService = EconomyService(mongoManager.database)
        GGNextAPI.economyService = economyService

        logger.info("GGNext Core enabled!")
    }

    override suspend fun onDisableAsync() {
        contentSystem.shutdown()
        mongoManager.close()

        logger.info("GGNext Core disabled!")
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(VanishCommand(VanishManager(this)).command)
        }
    }
}
