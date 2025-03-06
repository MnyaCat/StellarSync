package dev.mnyacat.stellar_sync_common.model

import dev.mnyacat.stellar_sync_common.config.ConfigManager
import org.apache.logging.log4j.Logger

abstract class StellarHolder<S> {
    var pluginEnable = false
    lateinit var logger: Logger
    abstract var storageWrapper: S
    lateinit var configManager: ConfigManager
}