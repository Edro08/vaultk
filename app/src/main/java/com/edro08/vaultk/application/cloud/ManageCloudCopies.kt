package com.edro08.vaultk.application.cloud

import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyRepository
import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudSyncLogEntry
import com.edro08.vaultk.domain.vault.OpenedVault
import com.edro08.vaultk.domain.vault.VaultContent
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
