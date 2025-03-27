package dev.mnyacat.stellar_sync_fabric.model

import dev.mnyacat.stellar_sync_common.model.StorageWrapperContext
import dev.mnyacat.stellar_sync_fabric.storage.FabricStorageWrapper

object FabricStorageContext: StorageWrapperContext<FabricStorageWrapper>() {
    override lateinit var storageWrapper: FabricStorageWrapper
}