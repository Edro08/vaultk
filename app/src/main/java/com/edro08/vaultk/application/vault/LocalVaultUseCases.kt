package com.edro08.vaultk.application.vault

import com.edro08.vaultk.domain.vault.VaultContent
import com.edro08.vaultk.domain.vault.VaultId
import com.edro08.vaultk.domain.vault.VaultRepository
import com.edro08.vaultk.domain.vault.OpenedVault
import com.edro08.vaultk.domain.vault.VaultSaveProgress
import com.edro08.vaultk.domain.vault.VaultSaveStage
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
