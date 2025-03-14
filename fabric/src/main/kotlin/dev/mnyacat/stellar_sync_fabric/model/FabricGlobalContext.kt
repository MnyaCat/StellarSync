package dev.mnyacat.stellar_sync_fabric.model

import dev.mnyacat.stellar_sync_common.model.GlobalContext
import dev.mnyacat.stellar_sync_fabric.storage.FabricStorageWrapper

object FabricGlobalContext : GlobalContext<FabricStorageWrapper>() {
    override lateinit var storageWrapper: FabricStorageWrapper
}