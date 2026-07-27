package de.ggnext.velocityCore.config

import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

data class VelocityConfig(
    var globalMaintenanceMode: Boolean = false,
    var maintenanceServers: List<String> = emptyList(),
    var mongoConnection: String = "mongodb://localhost:27017",
    var database: String = "ggnext",
    var prod: Boolean = false,
    var sentryDSN: String = "sentry",
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
                database = node.node("mongo", "database").getString("ggnext"),
                prod = node.node("prod", "prod").getBoolean(false),
                sentryDSN = node.node("sentry-dsn").getString("sentry-dsn"),
            )
    }

    fun save() {
        val node = loader.createNode()
        node.node("maintenance", "global").set(config.globalMaintenanceMode)
        node.node("maintenance", "servers").setList(String::class.java, config.maintenanceServers)
        node.node("mongo", "connection").set(config.mongoConnection)
        node.node("mongo", "database").set(config.database)
        node.node("prod", "prod").set(config.prod)
        node.node("sentry-dsn").set(config.sentryDSN)
        loader.save(node)
    }
}
