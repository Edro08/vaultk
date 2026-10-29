package com.edro08.vaultk.application

import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyRepository
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudSyncLogEntry
import com.edro08.vaultk.domain.OpenedVault
import com.edro08.vaultk.domain.VaultContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only presentation boundary until private downloads and deletion are wired up. */
class ManageCloudCopies(private val repository: CloudCopyRepository) {
    fun list(): List<CloudCopy> = repository.list()
    fun selected(): CloudCopy? = repository.selected()
    fun select(id: CloudCopyId?) = repository.select(id)
    fun recoverySnapshots(id: CloudCopyId): Int = repository.recoverySnapshots(id)
    fun logs(id: CloudCopyId? = null): List<CloudSyncLogEntry> = repository.logs(id)
    suspend fun remove(id: CloudCopyId) = withContext(Dispatchers.IO) { repository.remove(id) }
    suspend fun open(id: CloudCopyId, key: CharArray): VaultContent =
        withContext(Dispatchers.IO) { repository.open(id, key) }
    suspend fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault =
        withContext(Dispatchers.IO) { repository.openVersioned(id, key) }
}
