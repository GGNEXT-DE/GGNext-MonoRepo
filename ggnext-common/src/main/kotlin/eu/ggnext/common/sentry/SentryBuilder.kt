package eu.ggnext.common.sentry

import io.sentry.Sentry

object SentryBuilder {
    fun init(config: SentryConfig) {
        if (!config.prod) return
        require(!config.dsn.isNullOrBlank()) { "dsn is required" }
        Sentry.init { options ->
            options.dsn = config.dsn
        }
    }
}

data class SentryConfig(
    val dsn: String?,
    val prod: Boolean,
)
