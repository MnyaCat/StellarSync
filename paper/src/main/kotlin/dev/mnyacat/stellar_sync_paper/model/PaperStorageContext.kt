package dev.mnyacat.stellar_sync_paper.model

import dev.mnyacat.stellar_sync_common.model.StorageWrapperContext
import dev.mnyacat.stellar_sync_paper.storage.PaperStorageWrapper

object PaperStorageContext: StorageWrapperContext<PaperStorageWrapper>() {
    override lateinit var storageWrapper: PaperStorageWrapper
}