package de.ggnext.core.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import org.bukkit.configuration.Configuration

class MongoManager(
    private val config: Configuration,
) {
    private val mongoClient: MongoClient
    var database: MongoDatabase

    init {
        val connectionString =
            config.getString("mongo.connectionString")
                ?: throw IllegalStateException("MongoConnectionString property is missing")
        val databaseName = if (config.getBoolean("prod")) "production" else "staging"

        val settings =
            MongoClientSettings
                .builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(ConnectionString(connectionString))
                .build()

        mongoClient = MongoClient.create(settings)
        database = mongoClient.getDatabase(databaseName)
    }

    fun close() = mongoClient.close()
}
