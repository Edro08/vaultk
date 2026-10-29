package com.edro08.vaultk.application

import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyRepository
import com.edro08.vaultk.domain.OpenedVault
import com.edro08.vaultk.domain.VaultContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Commits a verified encrypted Pending copy; it never uploads or marks the remote as current. */
class SaveCloudCopyLocally(private val repository: CloudCopyRepository) {
    suspend operator fun invoke(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent): OpenedVault = withContext(Dispatchers.IO) {
        repository.savePending(id, expectedFingerprint, key, original, changed)
    }
}
