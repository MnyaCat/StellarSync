package dev.mnyacat.stellar_sync_common.model

import java.sql.Timestamp
import java.util.*

class PlayerData(
    val uuid: UUID,
    val inventory: String?,
    val enderChest: String?,
    val selectedSlot: Int?,
    val isOnline: Boolean,
    val lastServer: String,
    val hasCrashed: Boolean,
    val needsRollback: Boolean,
    val rollbackServer: String?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
) {}
