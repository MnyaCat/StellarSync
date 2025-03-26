package dev.mnyacat.stellar_sync_paper.listeners

import dev.mnyacat.stellar_sync_paper.model.PaperGlobalContext
import dev.mnyacat.stellar_sync_paper.model.PaperStorageContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerEventListener: Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        PaperStorageContext.storageWrapper.loadPlayerData(player, true)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        PaperStorageContext.storageWrapper.savePlayerData(
            player,
            isOnline = false,
            shouldSendFeedback = false,
            restoreCrash = false,
            restoreRollback = false
        )
    }
}