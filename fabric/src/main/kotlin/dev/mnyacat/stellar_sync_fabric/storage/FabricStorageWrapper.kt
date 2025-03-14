package dev.mnyacat.stellar_sync_fabric.storage

import dev.mnyacat.stellar_sync_common.model.PlayerData
import dev.mnyacat.stellar_sync_common.storage.ConnectionManager
import dev.mnyacat.stellar_sync_common.storage.Storage
import dev.mnyacat.stellar_sync_fabric.model.FabricGlobalContext
import dev.mnyacat.stellar_sync_fabric.parseNbtString
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.Logger
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


// MOD内ではこのクラスを通じてDBとやり取りする
class FabricStorageWrapper(
    logger: Logger,
    connectionManager: ConnectionManager,
    maxRetries: Int,
    retryDelayMs: Long,
    scheduler: ScheduledExecutorService,
    attempts: MutableMap<UUID, Int>
) :
    Storage<ServerPlayerEntity>(
        logger,
        connectionManager,
        maxRetries,
        retryDelayMs,
        scheduler,
        attempts
    ) {

    // 指定したプレイヤーのインベントリを保存する
    override fun savePlayerData(
        player: ServerPlayerEntity,
        isOnline: Boolean,
        shouldSendFeedback: Boolean,
        restoreCrash: Boolean,
        restoreRollback: Boolean,
    ) {
        if (!FabricGlobalContext.pluginEnable) {
            if (shouldSendFeedback) {
                player.sendMessage(
                    Text.literal("StellarSyncが無効化されているため, プレイヤーデータは保存されません: サーバー管理者に問い合わせてください.")
                        .formatted(Formatting.RED)
                )
            }
            logger.warn("Synchronization skipped. StellarSync is disabled.")
            return
        }
        val syncOptions = FabricGlobalContext.configManager.config.syncOptions
        val nbtInventory = getNbtInventory(player)
        val enderChest = getNbtEnderChest(player)
        val selectedSlot = player.inventory.selectedSlot
        val levelName = getLevelName(player)
        val uuid = player.uuid
        val attempt = attempts.getOrDefault(uuid, 0)
        try {
            val playerData = getPlayerDataFromDatabase(uuid)
            if (!restoreRollback && playerData.needsRollback) {
                if (shouldSendFeedback) {
                    player.sendMessage(
                        Text.literal("${playerData.rollbackServer}がロールバックされました:")
                            .formatted(Formatting.YELLOW)
                            .append(
                                Text.literal(" ${playerData.rollbackServer}へ接続するまで, インベントリは保存されません.")
                                    .formatted(Formatting.WHITE)
                            )
                    )
                }
                logger.warn("Player data synchronization for ${player.name} was not performed because ${playerData.rollbackServer} was rolled back. Synchronization will not resume until you connect to this server.")
                return
            }
            if (!restoreCrash && playerData.hasCrashed) {
                if (shouldSendFeedback) {
                    player.sendMessage(
                        Text.literal("前回プレイヤーデータの保存に失敗しています:")
                            .formatted(Formatting.RED)
                            .append(
                                Text.literal(" ${playerData.lastServer}へ接続するまで, インベントリは保存されません.")
                                    .formatted(Formatting.WHITE)
                            )
                    )
                }
                logger.warn("Player data synchronization for ${player.name} was not performed: because the inventory could not be saved properly on ${playerData.lastServer} last time, synchronization will not resume until you connect to this server.")
                return
            }
            savePlayerDataToDatabase(
                uuid,
                if (syncOptions.inventory) nbtInventory.toString() else null,
                if (syncOptions.enderChest) enderChest.toString() else null,
                if (syncOptions.selectedSlot) selectedSlot else null,
                isOnline,
                levelName,
                FabricGlobalContext.configManager.config.syncOptions
            )
            attempts.remove(uuid)
            if (shouldSendFeedback) {
                player.sendMessage(
                    Text.literal("プレイヤーデータを保存しました: $levelName -> Database")
                        .formatted(Formatting.GREEN)
                )
            }
            logger.info("Successfully saved player data for ${player.name}")
            return
        } catch (e: SQLException) {
            attempts[uuid] = attempt + 1
            if (attempt >= maxRetries) {
                attempts.remove(uuid)
                logger.error("Failed to save ${player.name}'s player data: must connect to $levelName on their next login.")
                logger.debug(e.message)
                return
            } else {
                logger.warn("Failed to save ${player.name}'s player data: Retrying...[$attempt/$maxRetries]")
                delayTask({ savePlayerData(player, isOnline) }, retryDelayMs, TimeUnit.MILLISECONDS)
            }
        }

    }

    override fun loadPlayerData(player: ServerPlayerEntity, isOnline: Boolean) {
        if (!FabricGlobalContext.pluginEnable) {
            player.sendMessage(
                Text.literal("StellarSyncが無効化されているため, プレイヤーデータは復元されません: サーバー管理者に問い合わせてください.")
                    .formatted(Formatting.RED)
            )
            logger.warn("Synchronization skipped. StellarSync is disabled.")
            return
        }
        val levelName = getLevelName(player)
        val uuid = player.uuid
        val attempt = attempts.getOrDefault(uuid, 0)
        try {
            val playerData: PlayerData
            var isCrashDetected = false
            try {
                playerData = getPlayerDataFromDatabase(uuid)
            } catch (e: NoSuchElementException) {
                val nbtInventory = getNbtInventory(player)
                val enderChest = getNbtEnderChest(player)
                val selectedSlot = player.inventory.selectedSlot
                initPlayerDataInDatabase(
                    uuid, nbtInventory.toString(),
                    enderChest.toString(), selectedSlot, true, levelName
                )
                attempts.remove(uuid)
                player.sendMessage(
                    Text.literal("プレイヤーデータを登録しました")
                        .formatted(Formatting.GREEN)
                )
                logger.info("Successfully initialized player data for ${player.name}")
                return
            }
            if (playerData.needsRollback) {
                if (playerData.rollbackServer == levelName) {
                    savePlayerData(player, isOnline = true, shouldSendFeedback = true, restoreRollback = true)
                    attempts.remove(uuid)
                    player.sendMessage(
                        Text.literal("データベースのデータをロールバックしたデータで上書きしました: [overwrite] $levelName -> Database")
                            .formatted(Formatting.GREEN)
                    )
                    logger.info(
                        "Successfully loaded player data for ${player.name}: Successfully loaded player data for ${player.name}: Previous save failure detected for ${player.name}; overwritten the existing player data in the database."
                    )
                    return
                } else {
                    attempts.remove(uuid)
                    // インベントリをクリアしてロールバックしたサーバーへの接続を促す
                    // TODO: ここでクリアしたインベントリはどこかに保存しておくべき？
                    player.inventory.clear()
                    player.sendMessage(
                        Text.literal("${playerData.rollbackServer}がロールバックされました:")
                            .formatted(Formatting.YELLOW)
                            .append(
                                Text.literal(" ${playerData.rollbackServer}へ接続してください")
                                    .formatted(Formatting.WHITE)
                            )
                    )
                    logger.warn("Player data synchronization for ${player.name} was not performed because ${playerData.rollbackServer} was rolled back. Synchronization will not resume until you connect to this server.")
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
                logger.info("Previous attempt to save player data failed. Marked ${player.name}'s 'hasCrashed' flag as true.")
            }
            if (playerData.hasCrashed || isCrashDetected) {
                if (playerData.lastServer == levelName) {
                    savePlayerData(player, isOnline = true, shouldSendFeedback = true, restoreCrash = true)
                    attempts.remove(uuid)
                    player.sendMessage(
                        Text.literal("前回プレイヤーデータの保存に失敗していたため, データベースのデータをサーバーのデータで上書きしました: [overwrite] $levelName -> Database")
                            .formatted(Formatting.GREEN)
                    )
                    logger.info(
                        "Successfully loaded player data for ${player.name}: Previous save failure detected for ${player.name}; overwritten the existing player data in the database."
                    )
                    return
                } else {
                    attempts.remove(uuid)
                    // インベントリをクリアして前回いたサーバーへの接続を促す
                    // TODO: ここでクリアしたインベントリはどこかに保存しておくべき？
                    player.inventory.clear()
                    player.sendMessage(
                        Text.literal("前回プレイヤーデータの保存に失敗しています:")
                            .formatted(Formatting.RED)
                            .append(
                                Text.literal(" ${playerData.lastServer}へ接続してください").formatted(Formatting.WHITE)
                            )
                    )
                    logger.warn("Player data synchronization for ${player.name} was not performed: because the inventory could not be saved properly on ${playerData.lastServer} last time, synchronization will not resume until you connect to this server.")
                    return
                }
            }
            logger.debug(playerData.enderChest?.javaClass?.name)
            val syncOptions = FabricGlobalContext.configManager.config.syncOptions
            if (syncOptions.inventory) {
                val inventoryData = playerData.inventory
                inventoryData?.let {
                    val nbtInventory = parseNbtString(inventoryData)
                    player.inventory.readNbt(nbtInventory)
                }
            }
            if (syncOptions.enderChest) {
                val enderChestData = playerData.enderChest
                enderChestData?.let {
                    val nbtEnderChest = parseNbtString(enderChestData)
                    player.enderChestInventory.readNbtList(nbtEnderChest, player.server.registryManager)
                }
            }
            if (syncOptions.selectedSlot) {
                val selectedSlotData = playerData.selectedSlot
                selectedSlotData?.let {
                    player.inventory.selectedSlot = selectedSlotData
                    player.networkHandler.sendPacket(UpdateSelectedSlotS2CPacket(selectedSlotData))
                }
            }
            updateOnlineStatus(player, isOnline, levelName)
            attempts.remove(uuid)
            player.sendMessage(
                Text.literal("プレイヤーデータを復元しました: Database(${playerData.lastServer}) -> $levelName")
                    .formatted(Formatting.GREEN)
            )
            logger.info("Successfully loaded player data for ${player.name}")
            return

        } catch (e: SQLException) {
            player.inventory.clear()
            if (attempt >= maxRetries) {
                attempts.remove(uuid)
                player.sendMessage(
                    Text.literal("プレイヤーデータの読み込みに失敗しました:")
                        .formatted(Formatting.RED)
                        .append(
                            Text.literal("リトライ回数が上限に達しました. サーバーに再接続してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.")
                                .formatted(Formatting.WHITE)
                        )
                )
                logger.error("Failed to load ${player.name}'s player data: The maximum number of retries has been reached. Please reconnect to the server.")
                logger.debug(e.message)
                return
            } else {
                attempts[uuid] = attempt + 1
                player.sendMessage(
                    Text.literal("プレイヤーデータの読み込みに失敗しました: ")
                        .formatted(Formatting.YELLOW)
                        .append(
                            Text.literal("リトライしています...[$attempt/$maxRetries]")
                                .formatted(Formatting.WHITE)
                        )
                )
                logger.warn("Failed to load ${player.name}'s player data: Retrying...[$attempt/$maxRetries]")
                delayTask({ loadPlayerData(player) }, retryDelayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun getLevelName(player: ServerPlayerEntity): String {
        // 取得できない場合は以下を参考にする
        // https://github.com/pugur523/MySQL_PlayerdataSync-4-Fabric/blob/main/src/main/java/com/pugur/playerdata/MySQLPlayerdataSync.java#L333
        return player.server.saveProperties.levelName
    }

    private fun getNbtInventory(player: ServerPlayerEntity): NbtList {
        val playerInventory = player.inventory
        val inventoryList = NbtList()
        return playerInventory.writeNbt(inventoryList)
    }

    private fun getNbtEnderChest(player: ServerPlayerEntity): NbtList {
        val enderChestInventory = player.enderChestInventory
        return enderChestInventory.toNbtList(player.server.registryManager)
    }

    fun updateFlags(player: ServerPlayerEntity, isOnline: Boolean, hasCrashed: Boolean, needsRollback: Boolean) {
        super.updateFlags(player.uuid, isOnline, hasCrashed, needsRollback)
    }

    fun updateOnlineFlag(player: ServerPlayerEntity, isOnline: Boolean) {
        super.updateOnlineFlag(player.uuid, isOnline)
    }

    fun updateLastServer(player: ServerPlayerEntity, lastServer: String) {
        super.updateLastServer(player.uuid, lastServer)
    }

    fun updateOnlineStatus(player: ServerPlayerEntity, isOnline: Boolean, lastServer: String) {
        super.updateOnlineStatus(player.uuid, isOnline, lastServer)
    }

    fun updateCrashFlag(player: ServerPlayerEntity, hasCrashed: Boolean) {
        super.updateCrashedFlag(player.uuid, hasCrashed)
    }

    fun updateRollbackFlag(player: ServerPlayerEntity, needsRollback: Boolean) {
        super.updateRollbackFlag(player.uuid, needsRollback)
    }

    fun updateRollbackServer(player: ServerPlayerEntity, rollbackServer: String) {
        super.updateRollbackServer(player.uuid, rollbackServer)
    }

    fun updateRollbackStatus(player: ServerPlayerEntity, needsRollback: Boolean, rollbackServer: String?) {
        super.updateRollbackStatus(player.uuid, needsRollback, rollbackServer)
    }

    fun updateRollbackStatus(players: Array<ServerPlayerEntity>, needsRollback: Boolean, rollbackServer: String?) {
        super.updateRollbackStatus(
            players.map { player -> player.uuid }.toTypedArray<UUID>(),
            needsRollback,
            rollbackServer
        )
    }
}