package com.edro08.vaultk.application.cloud

import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudCopyRepository
import com.edro08.vaultk.domain.vault.OpenedVault
import com.edro08.vaultk.domain.vault.VaultContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Commits a verified encrypted Pending copy; it never uploads or marks the remote as current. */
class SaveCloudCopyLocally(private val repository: CloudCopyRepository) {
    suspend operator fun invoke(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent): OpenedVault = withContext(Dispatchers.IO) {
        repository.savePending(id, expectedFingerprint, key, original, changed)
    }
}
