package de.ggnext.velocityCore.config

import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

data class VelocityConfig(
    var globalMaintenanceMode: Boolean = false,
    var maintenanceServers: List<String> = emptyList(),
    var mongoConnection: String = "mongodb://localhost:27017",
    var prod: Boolean = false,
)

class ConfigManager(
    dataDirectory: Path,
) {
    private val path = dataDirectory.resolve("config.yml")
    private val loader = YamlConfigurationLoader.builder().path(path).build()

    lateinit var config: VelocityConfig
        private set

    fun load() {
        val node = loader.load()
        config =
            VelocityConfig(
                globalMaintenanceMode = node.node("maintenance", "global").getBoolean(false),
                maintenanceServers = node.node("maintenance", "servers").getList(String::class.java) ?: emptyList(),
                mongoConnection = node.node("mongo", "connection").getString("mongodb://localhost:27017"),
                prod = node.node("prod", "prod").getBoolean(false),
            )
    }

    fun save() {
        val node = loader.createNode()
        node.node("maintenance", "global").set(config.globalMaintenanceMode)
        node.node("maintenance", "servers").setList(String::class.java, config.maintenanceServers)
        node.node("mongo", "connection").set(config.mongoConnection)
        node.node("prod", "prod").set(config.prod)
        loader.save(node)
    }
}
