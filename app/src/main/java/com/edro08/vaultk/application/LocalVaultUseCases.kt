package com.edro08.vaultk.application

import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultId
import com.edro08.vaultk.domain.VaultRepository
import com.edro08.vaultk.domain.OpenedVault
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenLocalVault(private val repository: VaultRepository) {
    suspend operator fun invoke(id: VaultId, key: CharArray): OpenedVault =
        withContext(Dispatchers.IO) { repository.open(id, key) }
}

class SaveLocalVault(private val repository: VaultRepository) {
    suspend operator fun invoke(id: VaultId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent,
        onProgress: (VaultSaveProgress) -> Unit): OpenedVault = withContext(Dispatchers.IO) {
        onProgress(VaultSaveProgress(VaultSaveStage.Preparing))
        repository.save(id, expectedFingerprint, key, original, changed, onProgress)
    }
}
