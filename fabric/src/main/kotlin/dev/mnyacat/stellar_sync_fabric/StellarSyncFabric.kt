package dev.mnyacat.stellar_sync_fabric

import dev.mnyacat.stellar_sync_common.config.ConfigManager
import dev.mnyacat.stellar_sync_common.setDebugLevel
import dev.mnyacat.stellar_sync_common.storage.ConnectionManager
import dev.mnyacat.stellar_sync_common.storage.DatabaseInitializer
import dev.mnyacat.stellar_sync_common.storage.DatabaseMigrator
import dev.mnyacat.stellar_sync_fabric.command.StellarSyncCommands
import dev.mnyacat.stellar_sync_fabric.command.StellarSyncDebugCommands
import dev.mnyacat.stellar_sync_fabric.model.FabricGlobalContext
import dev.mnyacat.stellar_sync_fabric.model.FabricMessageFormatter
import dev.mnyacat.stellar_sync_fabric.model.FabricStorageContext
import dev.mnyacat.stellar_sync_fabric.storage.FabricStorageWrapper
import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class StellarSyncFabric : ModInitializer {
    val logger = LogManager.getLogger()

    override fun onInitialize() {
        val configFilePath = "config/StellarSync.yaml"
        val configFile = File(configFilePath)
        val configFileExists = configFile.isFile && configFile.exists()
        FabricGlobalContext.configManager = ConfigManager(Paths.get(configFilePath))
        // TODO: configファイルが無かった場合、上でデフォルトの設定が生成されるのでMODを無効化する: サーバー全体へ通知
        // 無効化されている場合はユーザーへ通知する
        // セーブ、ロードなどの同期処理はこのフラグで機能をON/OFF
        if (!configFileExists) {
            // 初期をパスしてMODを無効化
            logger.warn("Default configuration generated because the configuration file was not found. Please modify the configuration file and restart the server.")
            return
        }
        val config = FabricGlobalContext.configManager.config
        FabricGlobalContext.logger = logger
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
        FabricStorageContext.storageWrapper = FabricStorageWrapper(
            logger,
            connectionManager,
            5,
            50L,
            Executors.newScheduledThreadPool(1),
            ConcurrentHashMap<UUID, Int>()
        )
        DatabaseInitializer.initialize(logger, connectionManager)
        DatabaseMigrator.migrate(logger, connectionManager)
        FabricGlobalContext.messageFormatter = FabricMessageFormatter()
        // register command
        /*val commands = StellarSyncCommands(logger)
        commands.onInitialize()*/
        if (config.debugMode) {
            setDebugLevel(logger)
            /*val debugCommands =
                StellarSyncDebugCommands(logger)
            debugCommands.onInitialize()*/
        }
        // MODを有効化
        FabricGlobalContext.pluginEnable = true
    }
}

