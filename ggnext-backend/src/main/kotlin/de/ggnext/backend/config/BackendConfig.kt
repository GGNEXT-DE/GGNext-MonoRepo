package de.ggnext.backend.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.nio.file.Paths

@ConfigSerializable
data class MongoSettings(
    var connection: String = "mongodb://localhost:27017",
    var database: String = "ggnext",
)

@ConfigSerializable
data class RedisSettings(
    var uri: String = "redis://127.0.0.1:6379",
)

@ConfigSerializable
data class BackendConfig(
    var mongo: MongoSettings = MongoSettings(),
    var redis: RedisSettings = RedisSettings(),
    var prod: Boolean = false,
    @Setting("sentry-dsn") var sentryDsn: String = "",
) {
    companion object {
        fun load(path: Path = Paths.get("config.yml")): BackendConfig {
            val loader = YamlConfigurationLoader.builder().path(path).build()
            val node = loader.load()
            val config = node.get(BackendConfig::class.java) ?: BackendConfig()
            runCatching {
                node.set(BackendConfig::class.java, config)
                loader.save(node)
            }
            System.getenv("MONGO_URI")?.let { config.mongo.connection = it }
            System.getenv("MONGO_DB")?.let { config.mongo.database = it }
            System.getenv("REDIS_URI")?.let { config.redis.uri = it }
            System.getenv("SENTRY_DSN")?.let { config.sentryDsn = it }
            System.getenv("PROD")?.let { config.prod = it.toBoolean() }
            return config
        }
    }
}
