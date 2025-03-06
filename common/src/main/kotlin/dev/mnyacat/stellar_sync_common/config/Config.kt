package dev.mnyacat.stellar_sync_common.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class Config(
    @Comment("Config version. Do not change this")
    val configVersion: String = "1.0",
    val database: DatabaseConfig = DatabaseConfig(),
    val syncOptions: SyncOptions = SyncOptions()
) {
    companion object {
        @ConfigSerializable
        data class DatabaseConfig(
            @Comment("format: jdbc:postgresql://<host>:<port>/<database>")
            val jdbcUrl: String = "jdbc:postgresql://<host>:<port>/<database>",
            val username: String = "username",
            val password: String = "password",
            val maximumPoolSize: Int = 5
        )

        @ConfigSerializable
        data class SyncOptions(
            val inventory: Boolean = true,
            val enderChest: Boolean = true,
            val selectedSlot: Boolean = true
        )
    }
}