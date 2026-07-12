package de.ggnext.velocityCore.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.ggnext.velocityCore.config.ConfigManager
import org.bson.UuidRepresentation

class MongoManager(
    private val configManager: ConfigManager,
) {
    private val mongoClient: MongoClient
    var database: MongoDatabase

    init {
        val connectionString = configManager.config.mongoConnection
        val databaseName = if (configManager.config.prod) "production" else "staging"

        val settings =
            MongoClientSettings
                .builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(ConnectionString(connectionString))
                .build()

        mongoClient = MongoClient.create(settings)
        database = mongoClient.getDatabase(databaseName)
    }
}
