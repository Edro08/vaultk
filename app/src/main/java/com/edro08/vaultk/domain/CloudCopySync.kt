package com.edro08.vaultk.domain

/** No OAuth token, file path or master password crosses this boundary. */
sealed interface CloudSyncOutcome {
    data class Verified(val copy: CloudCopy) : CloudSyncOutcome
    data class UpToDate(val copy: CloudCopy) : CloudSyncOutcome
    data class Downloaded(val copy: CloudCopy) : CloudSyncOutcome
    data class Rearmed(val copy: CloudCopy) : CloudSyncOutcome
    data object Conflict : CloudSyncOutcome
    data object Missing : CloudSyncOutcome
    data object StillPending : CloudSyncOutcome
}

enum class CloudConflictResolution {
    KeepLocal,
    UseRemote
}

interface CloudCopySync {
    fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit)
    fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit)
    fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        reconcile(id, callback)
    fun resolveConflict(id: CloudCopyId, resolution: CloudConflictResolution,
        callback: (Result<CloudSyncOutcome>) -> Unit) =
        reconcile(id, callback)
}
