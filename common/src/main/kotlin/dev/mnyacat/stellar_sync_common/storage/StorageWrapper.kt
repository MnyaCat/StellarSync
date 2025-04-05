package dev.mnyacat.stellar_sync_common.storage

import dev.mnyacat.stellar_sync_common.model.GlobalContext
import dev.mnyacat.stellar_sync_common.model.MessageColor
import dev.mnyacat.stellar_sync_common.model.PlayerData
import org.apache.logging.log4j.Logger
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


abstract class StorageWrapper<Player, MessageFormat>(
    logger: Logger,
    connectionManager: ConnectionManager,
    maxRetries: Int,
    retryDelayMs: Long,
    scheduler: ScheduledExecutorService,
    val attempts: MutableMap<UUID, Int>
): Storage<Player>(
    logger,
    connectionManager,
    maxRetries,
    retryDelayMs,
    scheduler
) {
    fun savePlayerData(
        player: Player,
        isOnline: Boolean,
        shouldSendFeedback: Boolean = false,
        restoreCrash: Boolean = false,
        restoreRollback: Boolean = false,
    ) {
        val context = getContext()
        val messageFormatter = context.messageFormatter
        if (!context.pluginEnable) {
            if (shouldSendFeedback) {
                val message = messageFormatter.literal("StellarSyncが無効化されているため, プレイヤーデータは保存されません: サーバー管理者に問い合わせてください.").color(MessageColor.RED)
                sendMessage(player, message.raw)
            }
            logger.warn("Synchronization skipped. StellarSync is disabled.")
            return
        }

        val syncOptions = context.configManager.config.syncOptions
        val inventory = getInventoryString(player)
        val enderChest = getEnderChestString(player)
        val selectedSlot = getSelectedSlot(player)
        val levelName = getLevelName(player)

        val uuid = getPlayerUUID(player)
        val playerName = getPlayerName(player)

        val attempt = attempts.getOrDefault(uuid, 0)
        try {
            val playerData = getPlayerDataFromDatabase(uuid)
            if (!restoreRollback && playerData.needsRollback) {
                if (shouldSendFeedback) {
                    val message = messageFormatter.literal("${playerData.rollbackServer}がロールバックされました:").color(MessageColor.YELLOW).append(messageFormatter.literal(" ${playerData.rollbackServer}へ接続するまで, インベントリは保存されません.").color(MessageColor.WHITE))
                    sendMessage(player, message.raw)
                }
                logger.warn("Player data synchronization for $playerName was not performed because ${playerData.rollbackServer} was rolled back. Synchronization will not resume until you connect to this server.")
                return
            }
            if (!restoreCrash && playerData.hasCrashed) {
                if (shouldSendFeedback) {
                    val message = messageFormatter.literal("前回プレイヤーデータの保存に失敗しています:").color(MessageColor.RED).append(messageFormatter.literal(" ${playerData.lastServer}へ接続するまで, インベントリは保存されません.").color(MessageColor.WHITE))
                    sendMessage(player, message.raw)
                }
                logger.warn("Player data synchronization for $playerName was not performed: because the inventory could not be saved properly on ${playerData.lastServer} last time, synchronization will not resume until you connect to this server.")
                return
            }
            savePlayerDataToDatabase(
                uuid,
                if (syncOptions.inventory) inventory else null,
                if (syncOptions.enderChest) enderChest else null,
                if (syncOptions.selectedSlot) selectedSlot else null,
                isOnline,
                levelName,
                context.configManager.config.syncOptions
            )
            attempts.remove(uuid)
            if (shouldSendFeedback) {
                val message = messageFormatter.literal("プレイヤーデータを保存しました: $levelName -> Database").color(MessageColor.GREEN)
                sendMessage(player, message.raw)
            }
            logger.info("Successfully saved player data for $playerName")
            return
        } catch (e: SQLException) {
            attempts[uuid] = attempt + 1
            if (attempt >= maxRetries) {
                attempts.remove(uuid)
                logger.error("Failed to save $playerName's player data: must connect to $levelName on their next login.")
                logger.debug(e.message)
                return
            } else {
                logger.warn("Failed to save $playerName's player data: Retrying...[$attempt/$maxRetries]")
                delayTask({ savePlayerData(player, isOnline) }, retryDelayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    fun loadPlayerData(player: Player, isOnline: Boolean = true) {
        val context = getContext()
        val messageFormatter = context.messageFormatter
        if (!context.pluginEnable) {
            val message = messageFormatter.literal("StellarSyncが無効化されているため, プレイヤーデータは復元されません: サーバー管理者に問い合わせてください.").color(MessageColor.RED)
            sendMessage(player, message.raw)
            logger.warn("Synchronization skipped. StellarSync is disabled.")
            return
        }

        val levelName = getLevelName(player)
        val playerName = getPlayerName(player)
        val uuid = getPlayerUUID(player)
        val attempt = attempts.getOrDefault(uuid, 0)
        try {
            val playerData: PlayerData
            var isCrashDetected = false
            try {
                playerData = getPlayerDataFromDatabase(uuid)
            } catch (e: NoSuchElementException) {
                val inventory = getInventoryString(player)
                val enderChest = getEnderChestString(player)
                val selectedSlot = getSelectedSlot(player)
                initPlayerDataInDatabase(
                    uuid, inventory,
                    enderChest, selectedSlot, true, levelName
                )
                attempts.remove(uuid)
                val message = messageFormatter.literal("プレイヤーデータを登録しました").color(MessageColor.GREEN)
                sendMessage(player, message.raw)
                logger.info("Successfully initialized player data for $playerName")
                return
            }
            if (playerData.needsRollback) {
                if (playerData.rollbackServer == levelName) {
                    savePlayerData(player, isOnline = true, shouldSendFeedback = true, restoreRollback = true)
                    attempts.remove(uuid)
                    val message = messageFormatter.literal("データベースのデータをロールバックしたデータで上書きしました: [overwrite] $levelName -> Database").color(MessageColor.GREEN)
                    sendMessage(player, message.raw)
                    logger.info(
                        "Successfully loaded player data for $playerName: Successfully loaded player data for $playerName: Previous save failure detected for $playerName; overwritten the existing player data in the database."
                    )
                    return
                } else {
                    attempts.remove(uuid)
                    // インベントリをクリアしてロールバックしたサーバーへの接続を促す
                    // TODO: ここでクリアしたインベントリはどこかに保存しておくべき？
                    clearInventory(player)
                    val message = messageFormatter.literal("${playerData.rollbackServer}がロールバックされました:").color(MessageColor.YELLOW).append(messageFormatter.literal(" ${playerData.rollbackServer}へ接続してください").color(MessageColor.WHITE))
                    sendMessage(player, message.raw)
                    logger.warn("Player data synchronization for $playerName was not performed because $playerName was rolled back. Synchronization will not resume until you connect to this server.")
                    return
                }
            }
            if (playerData.isOnline) {
                updateFlags(
                    uuid,
                    isOnline = false,
                    hasCrashed = true,
                    needsRollback = playerData.needsRollback
                )
                isCrashDetected = true
                logger.info("Previous attempt to save player data failed. Marked $playerName's 'hasCrashed' flag as true.")
            }
            if (playerData.hasCrashed || isCrashDetected) {
                if (playerData.lastServer == levelName) {
                    savePlayerData(player, isOnline = true, shouldSendFeedback = true, restoreCrash = true)
                    attempts.remove(uuid)
                    val message = messageFormatter.literal("前回プレイヤーデータの保存に失敗していたため, データベースのデータをサーバーのデータで上書きしました: [overwrite] $levelName -> Database").color(MessageColor.GREEN)
                    sendMessage(player, message.raw)
                    logger.info(
                        "Successfully loaded player data for $playerName: Previous save failure detected for $playerName; overwritten the existing player data in the database."
                    )
                    return
                } else {
                    attempts.remove(uuid)
                    // インベントリをクリアして前回いたサーバーへの接続を促す
                    // TODO: ここでクリアしたインベントリはどこかに保存しておくべき？
                    clearInventory(player)
                    val message = messageFormatter.literal("前回プレイヤーデータの保存に失敗しています:").color(MessageColor.RED).append(messageFormatter.literal(" ${playerData.lastServer}へ接続してください").color(MessageColor.WHITE))
                    sendMessage(player, message.raw)
                    logger.warn("Player data synchronization for $playerName was not performed: because the inventory could not be saved properly on ${playerData.lastServer} last time, synchronization will not resume until you connect to this server.")
                    return
                }
            }
            val syncOptions = context.configManager.config.syncOptions
            if (syncOptions.inventory) {
                val inventoryData = playerData.inventory
                inventoryData?.let {
                    loadInventory(player, inventoryData)
                }
            }
            if (syncOptions.enderChest) {
                val enderChestData = playerData.enderChest
                enderChestData?.let {
                    loadEnderChest(player, enderChestData)
                }
            }
            if (syncOptions.selectedSlot) {
                val selectedSlotData = playerData.selectedSlot
                selectedSlotData?.let {
                    loadSelectedSlot(player, selectedSlotData)
                }
            }
            updateOnlineStatus(getPlayerUUID(player), isOnline, levelName)
            attempts.remove(uuid)
            val message = messageFormatter.literal("プレイヤーデータを復元しました: Database(${playerData.lastServer}) -> $levelName").color(MessageColor.GREEN)
            sendMessage(player, message.raw)
            logger.info("Successfully loaded player data for $playerName")
            return

        } catch (e: SQLException) {
            clearInventory(player)
            if (attempt >= maxRetries) {
                attempts.remove(uuid)
                val message = messageFormatter.literal("プレイヤーデータの読み込みに失敗しました:").color(MessageColor.RED).append(messageFormatter.literal("リトライ回数が上限に達しました. サーバーに再接続してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.").color(MessageColor.WHITE))
                sendMessage(player, message.raw)
                logger.error("Failed to load $playerName's player data: The maximum number of retries has been reached. Please reconnect to the server.")
                logger.debug(e.message)
                return
            } else {
                attempts[uuid] = attempt + 1
                val message = messageFormatter.literal("プレイヤーデータの読み込みに失敗しました: ").color(MessageColor.YELLOW).append(messageFormatter.literal("リトライしています...[$attempt/$maxRetries]").color(MessageColor.WHITE))
                sendMessage(player, message.raw)
                logger.warn("Failed to load $playerName's player data: Retrying...[$attempt/$maxRetries]")
                delayTask({ loadPlayerData(player) }, retryDelayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    public override fun updateAllPlayersRollbackStatus(needsRollback: Boolean, rollbackServer: String?) {
        super.updateAllPlayersRollbackStatus(needsRollback, rollbackServer)
    }
    fun updateRollbackStatus(players: Array<Player>, needsRollback: Boolean, rollbackServer: String?) {
        val uuidArray = players.map { player -> getPlayerUUID(player) }.toTypedArray()
        super.updateRollbackStatus(uuidArray, needsRollback, rollbackServer)
    }

    fun updateFlags(player: Player, isOnline: Boolean, hasCrashed: Boolean, needsRollback: Boolean){
        super.updateFlags(getPlayerUUID(player), isOnline, hasCrashed, needsRollback)
    }
    fun updateOnlineFlag(player: Player, isOnline: Boolean) {
        super.updateOnlineFlag(getPlayerUUID(player), isOnline)
    }
    fun updateLastServer(player: Player, lastServer: String) {
        super.updateLastServer(getPlayerUUID(player), lastServer)
    }
    fun updateOnlineStatus(player: Player, isOnline: Boolean, lastServer: String) {
        super.updateOnlineStatus(getPlayerUUID(player), isOnline, lastServer)
    }
    fun updateCrashFlag(player: Player, hasCrashed: Boolean) {
        super.updateCrashedFlag(getPlayerUUID(player), hasCrashed)
    }
    fun updateRollbackFlag(player: Player, needsRollback: Boolean) {
        super.updateRollbackFlag(getPlayerUUID(player), needsRollback)
    }
    fun updateRollbackServer(player: Player, rollbackServer: String) {
        super.updateRollbackServer(getPlayerUUID(player), rollbackServer)
    }
    fun updateRollbackStatus(player: Player, needsRollback: Boolean, rollbackServer: String) {
        super.updateRollbackStatus(getPlayerUUID(player), needsRollback, rollbackServer)
    }


    protected abstract fun getContext(): GlobalContext<MessageFormat>

    protected abstract fun getPlayerUUID(player: Player): UUID
    protected abstract fun getPlayerName(player: Player): String
    protected abstract fun getInventoryString(player: Player): String
    protected abstract fun getEnderChestString(player: Player): String
    protected abstract fun getSelectedSlot(player: Player): Int
    protected abstract fun getLevelName(player: Player): String

    protected abstract fun sendMessage(player: Player, message: MessageFormat)

    protected abstract fun loadInventory(player: Player, inventory: String)
    protected abstract fun loadEnderChest(player: Player, enderChest: String)
    protected abstract fun loadSelectedSlot(player: Player, selectedSlot: Int)
    protected abstract fun clearInventory(player: Player)
}