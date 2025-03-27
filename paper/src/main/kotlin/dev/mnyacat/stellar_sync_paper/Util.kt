package dev.mnyacat.stellar_sync_paper

import de.tr7zw.nbtapi.NBT
import org.bukkit.inventory.Inventory

fun Inventory.toNbtString(): String {
    val nbtInventory = NBT.itemStackArrayToNBT(contents)
    return nbtInventory.toString()
}