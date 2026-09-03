package eu.ggnext.common.logging

import java.util.logging.Level
import java.util.logging.Logger

enum class LogLevel(
    internal val jul: Level,
) {
    DEBUG(Level.FINE),
    INFO(Level.INFO),
    WARN(Level.WARNING),
    ERROR(Level.SEVERE),
}

val Any.log: Logger
    get() = LogControl.logger(this::class.java)

fun Logger.debug(message: String) = fine(message)

fun Logger.info(message: String) = info(message)

fun Logger.warn(message: String) = warning(message)

fun Logger.error(message: String) = severe(message)

object LogControl {
    private const val ROOT = "de.ggnext"

    fun logger(clazz: Class<*>): Logger = Logger.getLogger(clazz.name)

    fun setLevel(level: LogLevel) {
        val logger = Logger.getLogger(ROOT)
        logger.level = level.jul
    }
}
