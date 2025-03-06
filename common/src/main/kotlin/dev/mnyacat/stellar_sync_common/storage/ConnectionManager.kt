package dev.mnyacat.stellar_sync_common.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class ConnectionManager(
    jdbcUrl: String,
    username: String,
    password: String,
    maximumPoolSize: Int

) {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
        }
        dataSource = HikariDataSource(config)
    }

    fun getConnection(): Connection {
        return dataSource.connection
    }

    fun shutdown() {
        dataSource.close()
    }
}