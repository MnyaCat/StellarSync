package dev.mnyacat.stellar_sync_common.storage

import dev.mnyacat.stellar_sync_common.config.Config
import dev.mnyacat.stellar_sync_common.model.PlayerData
import org.apache.logging.log4j.Logger
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

abstract class Storage<Player>(
    protected val logger: Logger,
    protected val connectionManager: ConnectionManager,
    protected val maxRetries: Int,
    protected val retryDelayMs: Long,
    protected val scheduler: ScheduledExecutorService
) {
    fun close() {
        connectionManager.close()
    }

    protected fun delayTask(task: Runnable, delay: Long, timeUnit: TimeUnit) {
        scheduler.schedule(task, delay, timeUnit)
    }

    // inventoryの初期値はどうするのか、どこで初期化をするのか
    @Throws(SQLException::class)
    protected fun initPlayerDataInDatabase(
        uuid: UUID,
        inventory: String?,
        enderChest: String?,
        selectedSlot: Int?,
        isOnline: Boolean,
        lastServer: String
    ) {
        val insertSQL = """
            INSERT INTO player_data (uuid, inventory, ender_chest, selected_slot, is_online, last_server) VALUES (?, ?, ?, ?, ?, ?);
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(insertSQL).use { statement ->
                with(statement) {
                    setObject(1, uuid)
                    inventory?.let { setString(2, inventory) } ?: run { setNull(2, java.sql.Types.NULL) }
                    enderChest?.let { setString(3, enderChest) } ?: run { setNull(3, java.sql.Types.NULL) }
                    selectedSlot?.let { setInt(4, it) } ?: run { setNull(4, java.sql.Types.NULL) }
                    setBoolean(5, isOnline)
                    setString(6, lastServer)
                }
                logger.debug("init player data: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun savePlayerDataToDatabase(
        uuid: UUID,
        inventory: String?,
        enderChest: String?,
        selectedSlot: Int?,
        isOnline: Boolean,
        lastServer: String,
        syncOptions: Config.Companion.SyncOptions
    ) {
        val syncInventory = syncOptions.inventory
        val syncEnderChest = syncOptions.enderChest
        val syncSelectedSlot = syncOptions.selectedSlot
        val upsertSQL = """
            INSERT INTO player_data (uuid, ${if (syncInventory) "inventory, " else ""}${if (syncEnderChest) "ender_chest, " else ""}${if (syncSelectedSlot) "selected_slot, " else ""}is_online, last_server, has_crashed, needs_rollback, rollback_server) VALUES (?, ${if (syncInventory) "?, " else ""}${if (syncEnderChest) "?, " else ""}${if (syncSelectedSlot) "?, " else ""}?, ?, false, false, NULL)
            ON CONFLICT (uuid)
            DO UPDATE SET
                ${if (syncInventory) "inventory = EXCLUDED.inventory," else ""}
                ${if (syncEnderChest) "ender_chest = EXCLUDED.ender_chest," else ""}
                ${if (syncSelectedSlot) "selected_slot = EXCLUDED.selected_slot," else ""}
                is_online = EXCLUDED.is_online,
                last_server = EXCLUDED.last_server,
                has_crashed = EXCLUDED.has_crashed,
                needs_rollback = EXCLUDED.needs_rollback,
                rollback_server = EXCLUDED.rollback_server,
                updated_at = CURRENT_TIMESTAMP;
        """.trimIndent()
        var parameterIndex = 1
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(upsertSQL).use { statement ->
                with(statement) {
                    setObject(parameterIndex++, uuid)
                    if (syncInventory) inventory?.let { setString(parameterIndex++, inventory) } ?: run {
                        setNull(
                            parameterIndex++,
                            java.sql.Types.NULL
                        )
                    }
                    if (syncEnderChest) enderChest?.let { setString(parameterIndex++, enderChest) } ?: run {
                        setNull(
                            parameterIndex++,
                            java.sql.Types.NULL
                        )
                    }
                    if (syncSelectedSlot) selectedSlot?.let { setInt(parameterIndex++, it) } ?: run {
                        setNull(
                            parameterIndex++,
                            java.sql.Types.NULL
                        )
                    }
                    setBoolean(parameterIndex++, isOnline)
                    setString(parameterIndex++, lastServer)
                }
                logger.debug("save player data: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateFlags(uuid: UUID, isOnline: Boolean, hasCrashed: Boolean, needsRollback: Boolean) {
        val updateSQL = """
        UPDATE player_data SET is_online = ?, has_crashed = ?, needs_rollback = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, isOnline)
                    setBoolean(2, hasCrashed)
                    setBoolean(3, needsRollback)
                    setObject(4, uuid)
                }
                logger.debug("update flags: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateOnlineFlag(uuid: UUID, isOnline: Boolean) {
        val updateSQL = """
        UPDATE player_data SET is_online = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, isOnline)
                    setObject(2, uuid)
                }
                logger.debug("update online flag: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateLastServer(uuid: UUID, lastServer: String) {
        val updateSQL = """
        UPDATE player_data SET last_server = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setString(1, lastServer)
                    setString(1, lastServer)
                    setObject(2, uuid)
                }
                logger.debug("update last server: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateOnlineStatus(uuid: UUID, isOnline: Boolean, lastServer: String) {
        val updateSQL = """
        UPDATE player_data SET is_online = ?, last_server = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, isOnline)
                    setString(2, lastServer)
                    setObject(3, uuid)
                }
                logger.debug("update online status: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateCrashedFlag(uuid: UUID, hasCrashed: Boolean) {
        val updateSQL = """
        UPDATE player_data SET has_crashed = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, hasCrashed)
                    setObject(2, uuid)
                }
                logger.debug("update crashed flag: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateRollbackFlag(uuid: UUID, needsRollback: Boolean) {
        val updateSQL = """
        UPDATE player_data SET needs_rollback = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, needsRollback)
                    setObject(2, uuid)
                }
                logger.debug("update rollback flag: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateRollbackServer(uuid: UUID, rollbackServer: String?) {
        val updateSQL = """
            UPDATE player_data SET rollback_server = ?, updated_at = CURRENT_TIMESTAMP
            WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    rollbackServer?.let { setString(1, rollbackServer) } ?: run { setNull(1, java.sql.Types.NULL) }
                    setObject(2, uuid)
                }
                logger.debug("update rollback server: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateRollbackStatus(uuid: UUID, needsRollback: Boolean, rollbackServer: String?) {
        val updateSQL = """
        UPDATE player_data SET needs_rollback = ?, rollback_server = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ?;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, needsRollback)
                    rollbackServer?.let { setString(2, rollbackServer) } ?: run { setNull(2, java.sql.Types.NULL) }
                    setObject(3, uuid)
                }
                logger.debug("update rollback status: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected fun updateRollbackStatus(uuidArray: Array<UUID>, needsRollback: Boolean, rollbackServer: String?) {
        val updateSQL = """
        UPDATE player_data SET needs_rollback = ?, rollback_server = ?, updated_at = CURRENT_TIMESTAMP
        WHERE uuid = ANY(?);
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            val sqlUuidArray = connection.createArrayOf("uuid", uuidArray)
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, needsRollback)
                    rollbackServer?.let { setString(2, rollbackServer) } ?: run { setNull(2, java.sql.Types.NULL) }
                    setArray(3, sqlUuidArray)
                }
                logger.debug("update rollback status: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    protected open fun updateAllPlayersRollbackStatus(needsRollback: Boolean, rollbackServer: String?) {
        val updateSQL = """
            UPDATE player_data SET needs_rollback = ?, rollback_server = ?, updated_at = CURRENT_TIMESTAMP;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSQL).use { statement ->
                with(statement) {
                    setBoolean(1, needsRollback)
                    rollbackServer?.let { setString(2, rollbackServer) } ?: run { setNull(2, java.sql.Types.NULL) }
                }
                logger.debug("update all players rollback status: {}", statement)
                statement.executeUpdate()
            }
        }
    }

    @Throws(NoSuchElementException::class, SQLException::class)
    protected fun getPlayerDataFromDatabase(uuid: UUID): PlayerData {
        val selectSql = """
            SELECT uuid, inventory, ender_chest, selected_slot, is_online, last_server, has_crashed, needs_rollback, rollback_server, created_at, updated_at FROM player_data WHERE uuid = ? LIMIT 1;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(selectSql).use { statement ->
                statement.setObject(1, uuid)
                logger.debug("get player data: {}", statement)
                val results = statement.executeQuery()
                if (results.next()) {
                    return PlayerData(
                        uuid = UUID.fromString(results.getString("uuid")),
                        inventory = results.getString("inventory"),
                        enderChest = results.getString("ender_chest"),
                        selectedSlot = results.getInt("selected_slot"),
                        isOnline = results.getBoolean("is_online"),
                        lastServer = results.getString("last_server"),
                        hasCrashed = results.getBoolean("has_crashed"),
                        needsRollback = results.getBoolean("needs_rollback"),
                        rollbackServer = results.getString("rollback_server"),
                        createdAt = results.getTimestamp("created_at"),
                        updatedAt = results.getTimestamp("updated_at")
                    )
                } else {
                    throw NoSuchElementException("No player data found for UUID: $uuid")
                }
            }
        }
    }
}