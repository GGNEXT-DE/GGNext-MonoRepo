package eu.ggnext.velocityCore.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

@ConfigSerializable
data class Maintenance(
    var global: Boolean = false,
    var servers: List<String> = emptyList(),
)

@ConfigSerializable
data class Mongo(
    var connection: String = "mongodb://localhost:27017",
    var database: String = "ggnext",
)

@ConfigSerializable
data class VelocityConfig(
    var maintenance: Maintenance = Maintenance(),
    var mongo: Mongo = Mongo(),
    var prod: Boolean = false,
    @Setting("sentry-dsn") var sentryDSN: String = "sentry",
)

class ConfigManager(
    dataDirectory: Path,
) {
    private val path = dataDirectory.resolve("config.yml")
    private val loader = YamlConfigurationLoader.builder().path(path).build()

    lateinit var config: VelocityConfig
        private set

    fun load() {
        config = loader.load().get(VelocityConfig::class.java) ?: VelocityConfig()
    }

    fun save() {
        val node = loader.createNode()
        node.set(VelocityConfig::class.java, config)
        loader.save(node)
    }
}
