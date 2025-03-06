package dev.mnyacat.stellar_sync_common.storage

import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


abstract class StorageWrapper<Player> {
    protected abstract val logger: Logger
    abstract val maxRetries: Int
    abstract val retryDelayMs: Long
    protected abstract val scheduler: ScheduledExecutorService
    protected abstract val attempts: MutableMap<UUID, Int>

    abstract fun savePlayerData(player: Player, isOnline: Boolean): Unit
    abstract fun loadPlayerData(player: Player): Unit
    abstract fun getLevelName(player: Player): String

    protected fun delayTask(task: Runnable, delay: Long, timeUnit: TimeUnit) {
        scheduler.schedule(task, delay, timeUnit)
    }
}