package dev.mnyacat.stellar_sync_common.storage

import org.apache.logging.log4j.Logger
import java.sql.SQLException

class SchemaVersion(val major: Int, val minor: Int)

object DatabaseMigrator {
    private var migrated = false

    // メジャーバージョンを上げる時は「テーブルを新しく作り直す時」
    // example
    /*@Throws(SQLException::class)
    private fun migrateToV0_2(logger: Logger, connectionManager: ConnectionManager) {
        connectionManager.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val migrateSQL = """
                    ALTER TABLE player_data
                    ADD COLUMN needs_rollback BOOLEAN NOT NULL DEFAULT false;
                """.trimIndent()
                logger.debug("migrate: migrating to version 0.2...: {}", migrateSQL)
                statement.executeUpdate(migrateSQL)
            }
            updateSchemaVersion(logger, connectionManager, SchemaVersion(0, 1))
            logger.info("Migration to version 0.2 completed successfully.")
        }
    }*/

    fun migrate(logger: Logger, connectionManager: ConnectionManager) {
        if (migrated) {
            logger.info("migration already performed, skipping process.")
            return
        }
        var version = getCurrentSchemaVersion(logger, connectionManager)
        // example
        /*if (version.major == 0 && version.minor < 2) {
            migrateToV0_2(logger, connectionManager)
            version = SchemaVersion(0, 2)
        }*/
    }

    private fun updateSchemaVersion(
        logger: Logger,
        connectionManager: ConnectionManager,
        schemaVersion: SchemaVersion
    ) {
        val updateSchemaVersionSQL = """
            UPDATE schema_version SET major = ?, minor = ?, applied_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = 1;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(updateSchemaVersionSQL).use { statement ->
                statement.setInt(1, schemaVersion.major)
                statement.setInt(2, schemaVersion.minor)
                statement.executeUpdate()
                try {
                    statement.executeUpdate()
                    logger.debug("migrate: update schema version: {}", statement)
                } catch (e: SQLException) {
                    logger.error(e.message)
                }
            }
        }
    }

    @Throws(NoSuchElementException::class)
    private fun getCurrentSchemaVersion(logger: Logger, connectionManager: ConnectionManager): SchemaVersion {
        val insertSQL = """
            SELECT major, minor FROM schema_version WHERE id = 1 LIMIT 1;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.prepareStatement(
                insertSQL
            ).use { statement ->
                logger.debug("migrate: get schema version: {}", statement)
                val result = statement.executeQuery()
                if (result.next()) {
                    val major = result.getInt("major")
                    val minor = result.getInt("minor")
                    return SchemaVersion(major, minor)
                } else {
                    throw NoSuchElementException("schema version not found")
                }
            }
        }

    }
}