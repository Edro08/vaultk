package com.edro08.vaultk.domain.cloud

import java.io.InputStream
import com.edro08.vaultk.domain.vault.OpenedVault
import com.edro08.vaultk.domain.vault.VaultContent

/** No local path is part of a cloud copy's public identity. */
data class CloudCopyId(val accountId: String, val driveId: String, val itemId: String)

enum class CloudCopyState { Clean, Pending, Conflict }
class CloudCopyProtectedException : Exception("Cloud copy is not clean")

data class CloudCopyConflict(
    val baseEtag: String,
    val localBytes: Long,
    val localSavedAt: Long,
    val remoteEtag: String,
    val remoteBytes: Long,
    val detectedAt: Long
)

enum class CloudSyncAction {
    PromoteDownload,
    SavePending,
    UploadClaimed,
    UploadVerified,
    UploadRearmed,
    BaseAdvanced,
    RemoteDownloaded,
    ConflictDetected,
    ResolvedKeepLocal,
    ResolvedUseRemote
}

/** Operation log entry without master passwords, tokens, URLs or local file paths. */
data class CloudSyncLogEntry(
    val timestamp: Long,
    val copyId: CloudCopyId,
    val action: CloudSyncAction,
    val state: CloudCopyState,
    val bytes: Long,
    val detail: String = ""
)

data class CloudCopy(val id: CloudCopyId, val name: String, val eTag: String,
    val bytes: Long, val savedAt: Long, val state: CloudCopyState,
    val accountName: String = "", val uploadAttempted: Boolean = false,
    val conflict: CloudCopyConflict? = null)

interface CloudCopyRepository {
    fun list(): List<CloudCopy>
    fun selected(): CloudCopy?
    fun select(id: CloudCopyId?)
    /** Removes only this device's clean, unselected copy; never calls Graph. */
    fun remove(id: CloudCopyId)
    /** Only decrypts in memory using the existing KDBX reader. Never grants a writable file path. */
    fun open(id: CloudCopyId, key: CharArray): VaultContent
    /** For a future editor: compare the exact encrypted base before preparing a local change. */
    fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault
    /** Persists an encrypted KDBX and Pending before any network request is permitted. */
    fun savePending(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent): OpenedVault
    /** Installs encrypted KDBX bytes and metadata; refuses to replace non-clean copies. */
    fun promote(copy: CloudCopy, source: InputStream): CloudCopy
    /** Counts encrypted recovery KDBX snapshots retained alongside the active blob. */
    fun recoverySnapshots(id: CloudCopyId): Int = 0
    /** Returns recent non-secret operation log entries, newest first. */
    fun logs(id: CloudCopyId? = null): List<CloudSyncLogEntry> = emptyList()
}

/** Stable biometric identity, independent of filename, path and ETag. */
fun CloudCopyId.biometricReference(): String =
    "onedrive:${accountId.length}:$accountId:${driveId.length}:$driveId:${itemId.length}:$itemId"
