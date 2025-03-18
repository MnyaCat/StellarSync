package dev.mnyacat.stellar_sync_common.model

import dev.mnyacat.stellar_sync_common.config.ConfigManager
import org.apache.logging.log4j.Logger

abstract class GlobalContext<S> {
    var pluginEnable = false
    lateinit var logger: Logger
    lateinit var configManager: ConfigManager
}