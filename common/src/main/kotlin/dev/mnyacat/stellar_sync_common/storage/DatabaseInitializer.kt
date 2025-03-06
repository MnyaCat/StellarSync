package dev.mnyacat.stellar_sync_common.storage

import org.apache.logging.log4j.Logger
import java.sql.SQLException

object DatabaseInitializer {
    private var initialized = false
    val schemaVersion = SchemaVersion(0, 2)

    fun initialize(logger: Logger, connectionManager: ConnectionManager) {
        if (initialized) {
            logger.info("Initialization already performed, skipping process.")
            return
        }
        // updated_atはプログラム側で制御
        val createPlayerDataTableSQL = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid UUID PRIMARY KEY NOT NULL,
                inventory TEXT,
                ender_chest TEXT,
                selected_slot INT,
                is_online BOOLEAN NOT NULL,
                last_server varchar(256) NOT NULL,
                has_crashed BOOLEAN NOT NULL DEFAULT false,
                needs_rollback BOOLEAN NOT NULL DEFAULT false,
                rollback_server varchar(256) DEFAULT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent()
        val createSchemaVersionTableSQL = """
            CREATE TABlE IF NOT EXISTS schema_version (
                id INT PRIMARY KEY DEFAULT 1,
                major INT NOT NULL,
                minor INT NOT NULL,
                applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent()
        val upsertSchemaVersionSQL = """
            INSERT INTO schema_version (id, major, minor) VALUES (?, ?, ?)
            ON CONFLICT (id)
            DO NOTHING;
        """.trimIndent()
        connectionManager.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                try {
                    statement.executeUpdate(createPlayerDataTableSQL)
                    logger.debug("init - create player data table: {}", statement)
                } catch (e: SQLException) {
                    logger.error(e.message)
                }
            }
            connection.createStatement().use { statement ->
                try {
                    statement.executeUpdate(createSchemaVersionTableSQL)
                    logger.debug("init - create schema version table: {}", statement)
                } catch (e: SQLException) {
                    logger.error(e.message)
                }
            }
            connection.prepareStatement(upsertSchemaVersionSQL).use { statement ->
                statement.setInt(1, 1)
                statement.setInt(2, schemaVersion.major)
                statement.setInt(3, schemaVersion.minor)
                statement.executeUpdate()
                try {
                    statement.executeUpdate()
                    logger.debug("init - register schema version: {}", statement)
                } catch (e: SQLException) {
                    logger.error(e.message)
                }
            }
        }
        logger.info("Initialization completed successfully.")
    }
}