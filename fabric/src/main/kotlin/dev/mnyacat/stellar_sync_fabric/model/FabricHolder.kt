package dev.mnyacat.stellar_sync_fabric.model

import dev.mnyacat.stellar_sync_common.model.StellarHolder
import dev.mnyacat.stellar_sync_fabric.storage.FabricStorageWrapper

object FabricHolder : StellarHolder<FabricStorageWrapper>() {
    override lateinit var storageWrapper: FabricStorageWrapper
}