package com.edro08.vaultk.application

import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopySync
import com.edro08.vaultk.domain.CloudSyncOutcome

/** Kept separate from the read-only UI until a synthetic KDBX end-to-end test passes. */
class SynchronizeCloudCopy(private val sync: CloudCopySync) {
    operator fun invoke(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        sync.synchronize(id, callback)
    fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        sync.reconcile(id, callback)
    fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        sync.refresh(id, callback)
    fun resolveConflict(id: CloudCopyId, resolution: CloudConflictResolution,
        callback: (Result<CloudSyncOutcome>) -> Unit) =
        sync.resolveConflict(id, resolution, callback)
}
