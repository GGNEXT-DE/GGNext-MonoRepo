package eu.ggnext.common.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation

class MongoManager(
    private val connectionString: String?,
    private val databaseName: String?,
) {
    private val mongoClient: MongoClient
    val database: MongoDatabase

    init {
        require(!connectionString.isNullOrBlank()) { "connectionString cannot be null or blank" }
        require(!databaseName.isNullOrBlank()) { "databaseName cannot be null or blank" }

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
