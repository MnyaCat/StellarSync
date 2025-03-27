package dev.mnyacat.stellar_sync_paper

import dev.mnyacat.stellar_sync_common.config.ConfigManager
import dev.mnyacat.stellar_sync_common.setDebugLevel
import dev.mnyacat.stellar_sync_common.storage.ConnectionManager
import dev.mnyacat.stellar_sync_common.storage.DatabaseInitializer
import dev.mnyacat.stellar_sync_common.storage.DatabaseMigrator
import dev.mnyacat.stellar_sync_paper.listeners.PlayerEventListener
import dev.mnyacat.stellar_sync_paper.model.PaperGlobalContext
import dev.mnyacat.stellar_sync_paper.model.PaperMessageFormatter
import dev.mnyacat.stellar_sync_paper.model.PaperStorageContext
import dev.mnyacat.stellar_sync_paper.storage.PaperStorageWrapper
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


class StellarSyncPaper : JavaPlugin() {
    val logger = LogManager.getLogger()

    override fun onEnable() {
        val configFilePath = "plugins/StellarSync/StellarSync.yaml"
        val configFile = File(configFilePath)
        val configFileExists = configFile.isFile && configFile.exists()
        PaperGlobalContext.configManager = ConfigManager(Paths.get(configFilePath))
        // 無効化されている場合はユーザーへ通知する
        // セーブ、ロードなどの同期処理はこのフラグで機能をON/OFF
        if (!configFileExists) {
            // 初期をパスしてMODを無効化
            logger.warn("Default configuration generated because the configuration file was not found. Please modify the configuration file and restart the server.")
            return
        }
        val config = PaperGlobalContext.configManager.config
        PaperGlobalContext.logger = logger
        val connectionManager: ConnectionManager
        try {
            connectionManager = ConnectionManager(
                config.database.jdbcUrl,
                config.database.username,
                config.database.password,
                config.database.maximumPoolSize
            )
        } catch (e: RuntimeException) {
            // データベースへの接続失敗
            logger.error("StellarSync disabled due to failure to connect to the database.: {}", e.message)
            return
        }
        PaperStorageContext.storageWrapper = PaperStorageWrapper(
            logger,
            connectionManager,
            5,
            50L,
            Executors.newScheduledThreadPool(1),
            ConcurrentHashMap<UUID, Int>()
        )
        DatabaseInitializer.initialize(logger, connectionManager)
        DatabaseMigrator.migrate(logger, connectionManager)
        PaperGlobalContext.messageFormatter = PaperMessageFormatter()
        if (config.debugMode) {
            setDebugLevel(logger)
        }
        // register command
        PaperGlobalContext.pluginEnable = true
        server.pluginManager.registerEvents(PlayerEventListener(), this)
    }

    override fun onDisable() {
        for (player in Bukkit.getOnlinePlayers()) {
            PaperStorageContext.storageWrapper.savePlayerData(player, false)
        }
        PaperStorageContext.storageWrapper.close()
    }
}
