package eu.ggnext.common.sentry

import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn
import io.sentry.Sentry

object SentryBuilder {
    fun init(config: SentryConfig) {
        if (!config.prod) return
        if (config.dsn.isNullOrBlank()) {
            log.warn("Dsn is required")
            return
        }
        Sentry.init { options ->
            options.dsn = config.dsn
        }
    }
}

data class SentryConfig(
    val dsn: String?,
    val prod: Boolean,
)
