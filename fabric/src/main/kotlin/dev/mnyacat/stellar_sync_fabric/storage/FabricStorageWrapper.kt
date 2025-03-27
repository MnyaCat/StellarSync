package dev.mnyacat.stellar_sync_fabric.storage

import dev.mnyacat.stellar_sync_common.model.GlobalContext
import dev.mnyacat.stellar_sync_common.storage.ConnectionManager
import dev.mnyacat.stellar_sync_common.storage.StorageWrapper
import dev.mnyacat.stellar_sync_fabric.model.FabricGlobalContext
import dev.mnyacat.stellar_sync_fabric.parseNbtString
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.ScheduledExecutorService


// MOD内ではこのクラスを通じてDBとやり取りする
class FabricStorageWrapper(
    logger: Logger,
    connectionManager: ConnectionManager,
    maxRetries: Int,
    retryDelayMs: Long,
    scheduler: ScheduledExecutorService,
    attempts: MutableMap<UUID, Int>
) :
    StorageWrapper<ServerPlayerEntity, Text>(
        logger,
        connectionManager,
        maxRetries,
        retryDelayMs,
        scheduler,
        attempts
    ) {

    override fun getContext(): GlobalContext<Text> {
        return FabricGlobalContext
    }

    override fun getPlayerUUID(player: ServerPlayerEntity): UUID {
        return player.uuid
    }
    override fun getPlayerName(player: ServerPlayerEntity): String {
        return player.name.toString()
    }
    override fun getInventoryString(player: ServerPlayerEntity): String {
        val inventoryList = NbtList()
        return player.inventory.writeNbt(inventoryList).toString()
    }
    override fun getEnderChestString(player: ServerPlayerEntity): String {
        return player.enderChestInventory.toNbtList(player.server.registryManager).toString()
    }
    override fun getSelectedSlot(player: ServerPlayerEntity): Int {
        return player.inventory.selectedSlot
    }
    override fun getLevelName(player: ServerPlayerEntity): String {
        // 取得できない場合は以下を参考にする
        // https://github.com/pugur523/MySQL_PlayerdataSync-4-Fabric/blob/main/src/main/java/com/pugur/playerdata/MySQLPlayerdataSync.java#L333
        return player.server.saveProperties.levelName
    }

    override fun sendMessage(player: ServerPlayerEntity, message: Text) {
        player.sendMessage(message)
    }

    override fun loadInventory(player: ServerPlayerEntity, inventory: String) {
        val nbtInventory = parseNbtString(inventory)
        player.inventory.readNbt(nbtInventory)
    }
    override fun loadEnderChest(player: ServerPlayerEntity, enderChest: String) {
        val nbtEnderChest = parseNbtString(enderChest)
        player.enderChestInventory.readNbtList(nbtEnderChest, player.server.registryManager)
    }
    override fun loadSelectedSlot(player: ServerPlayerEntity, selectedSlot: Int) {
        player.inventory.selectedSlot = selectedSlot
        player.networkHandler.sendPacket(UpdateSelectedSlotS2CPacket(selectedSlot))
    }

    override fun clearInventory(player: ServerPlayerEntity) {
        player.inventory.clear()
    }
}