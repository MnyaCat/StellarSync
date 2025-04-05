package dev.mnyacat.stellar_sync_paper.storage

import de.tr7zw.nbtapi.NBT
import dev.mnyacat.stellar_sync_common.model.GlobalContext
import dev.mnyacat.stellar_sync_common.storage.ConnectionManager
import dev.mnyacat.stellar_sync_common.storage.StorageWrapper
import dev.mnyacat.stellar_sync_paper.model.PaperGlobalContext
import dev.mnyacat.stellar_sync_paper.toNbtString
import net.kyori.adventure.text.Component
import org.apache.logging.log4j.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ScheduledExecutorService

class PaperStorageWrapper(
    logger: Logger,
    connectionManager: ConnectionManager,
    maxRetries: Int,
    retryDelayMs: Long,
    scheduler: ScheduledExecutorService,
    attempts: MutableMap<UUID, Int>,
    waitedCounts: MutableMap<UUID, Int>
) : StorageWrapper<Player, Component>(
    logger,
    connectionManager,
    maxRetries,
    retryDelayMs,
    scheduler,
    attempts,
    waitedCounts
) {
    override fun getContext(): GlobalContext<Component> {
        return PaperGlobalContext
    }

    override fun getPlayerUUID(player: Player): UUID {
        return player.uniqueId
    }

    override fun getPlayerName(player: Player): String {
        return player.name
    }

    /// ref: https://github.com/pugur523/MySQL_PlayerdataSync-4-Paper/blob/84736968c7ee7e0f8acda14b4fe3793fb8a7883f/src/main/java/com/pugur/playerdatasync/MySQLPlayerdataSync.java#L226
    override fun getInventoryString(player: Player): String {
        val inventory = player.inventory
        val jsonInventory = inventory.toNbtString()
            .replace("Slot:36", "Slot:100")
            .replace("Slot:37", "Slot:101")
            .replace("Slot:38", "Slot:102")
            .replace("Slot:39", "Slot:103")
            .replace("Slot:40", "Slot:-106")
        return formatInventory(jsonInventory)
    }

    override fun getEnderChestString(player: Player): String {
        val enderChest = player.enderChest
        return formatInventory(enderChest.toNbtString())
    }

    override fun getSelectedSlot(player: Player): Int {
        return player.inventory.heldItemSlot
    }

    override fun getLevelName(player: Player): String {
        val worlds = Bukkit.getWorlds()
        return worlds[0].name
    }

    override fun sendMessage(player: Player, message: Component) {
        player.sendMessage(message)
    }

    /// ref: https://github.com/pugur523/MySQL_PlayerdataSync-4-Paper/blob/84736968c7ee7e0f8acda14b4fe3793fb8a7883f/src/main/java/com/pugur/playerdatasync/MySQLPlayerdataSync.java#L261
    override fun loadInventory(player: Player, inventory: String) {
        val formattedInventory = inventory
            .replace("Slot:100","Slot:36")
            .replace("Slot:101", "Slot:37")
            .replace("Slot:102", "Slot:38")
            .replace("Slot:103", "Slot:39")
            .replace("Slot:-106", "Slot:40")
        val nbt = NBT.parseNBT("{items:$formattedInventory,size:41}")
        val itemStack = NBT.itemStackArrayFromNBT(nbt)
        if (itemStack != null) {
            player.inventory.contents = itemStack
        }
    }

    override fun loadEnderChest(player: Player, enderChest: String) {
        val nbt = NBT.parseNBT("{items:$enderChest,size:27}")
        val itemStack = NBT.itemStackArrayFromNBT(nbt)
        if (itemStack != null) {
            player.enderChest.contents = itemStack
        }
    }

    override fun loadSelectedSlot(player: Player, selectedSlot: Int) {
        player.inventory.heldItemSlot = selectedSlot
    }

    override fun clearInventory(player: Player) {
        player.inventory.clear()
    }

    /// ref: https://github.com/pugur523/MySQL_PlayerdataSync-4-Paper/blob/84736968c7ee7e0f8acda14b4fe3793fb8a7883f/src/main/java/com/pugur/playerdatasync/MySQLPlayerdataSync.java#L247
    fun formatInventory(inventory: String): String {
        try {
            val beginIndex = inventory.indexOf("[")
            val endIndex = inventory.indexOf(",size")
            return inventory.substring(beginIndex, endIndex)
        } catch (e: Exception) {
            return "[]"
        }
    }
}