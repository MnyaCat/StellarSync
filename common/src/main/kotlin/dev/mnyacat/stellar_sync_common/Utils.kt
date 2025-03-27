package dev.mnyacat.stellar_sync_common

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.Configurator

fun setLogLevel(logger: Logger, logLevel: Level) {
    Configurator.setLevel(logger.name, logLevel)
}

fun setDebugLevel(logger: Logger) {
    setLogLevel(logger, Level.DEBUG)
}