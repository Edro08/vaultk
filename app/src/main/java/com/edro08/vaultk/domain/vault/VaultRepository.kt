package com.edro08.vaultk.domain.vault

/** Opaque local identifier. Only the local adapter interprets its value as a path. */
@JvmInline value class VaultId(val value: String)

data class OpenedVault(val content: VaultContent, val fingerprint: ByteArray)

enum class VaultSaveStage { Preparing, Writing, Verifying, Replacing }

data class VaultSaveProgress(val stage: VaultSaveStage, val writtenFraction: Float? = null)

/** Persistence contract owned by the core, implemented by the KDBX adapter. */
interface VaultRepository {
    fun open(id: VaultId, key: CharArray): OpenedVault
    fun save(id: VaultId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent,
        onProgress: (VaultSaveProgress) -> Unit): OpenedVault
}
