package com.edro08.vaultk

import com.edro08.vaultk.application.SynchronizeCloudCopy
import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudCopySync
import com.edro08.vaultk.domain.CloudSyncOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SynchronizeCloudCopyTest {
    @Test fun separatesOneShotSynchronizeFromExplicitReconcile() {
        val id = CloudCopyId("account", "drive", "item")
        val rearmed = CloudCopy(id, "synthetic.kdbx", "base-v1", 12L, 1L,
            CloudCopyState.Pending, uploadAttempted = false)
        val verified = rearmed.copy(eTag = "new-v2", state = CloudCopyState.Clean)
        var syncCalls = 0
        var reconcileCalls = 0
        var refreshCalls = 0
        var resolvedWith: CloudConflictResolution? = null
        val port = object : CloudCopySync {
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                syncCalls++
                callback(Result.success(CloudSyncOutcome.Verified(verified)))
            }
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                reconcileCalls++
                callback(Result.success(CloudSyncOutcome.Rearmed(rearmed)))
            }
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                refreshCalls++
                callback(Result.success(CloudSyncOutcome.Downloaded(verified)))
            }
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) {
                resolvedWith = resolution
                callback(Result.success(CloudSyncOutcome.Verified(verified)))
            }
        }
        val useCase = SynchronizeCloudCopy(port)
        useCase.reconcile(id) { assertEquals(CloudSyncOutcome.Rearmed(rearmed), it.getOrThrow()) }
        assertEquals(0, syncCalls)
        assertEquals(1, reconcileCalls)
        assertEquals(0, refreshCalls)
        useCase.refresh(id) { assertEquals(CloudSyncOutcome.Downloaded(verified), it.getOrThrow()) }
        assertEquals(0, syncCalls)
        assertEquals(1, reconcileCalls)
        assertEquals(1, refreshCalls)
        useCase(id) { assertEquals(CloudSyncOutcome.Verified(verified), it.getOrThrow()) }
        assertEquals(1, syncCalls)
        assertEquals(1, reconcileCalls)
        assertEquals(1, refreshCalls)
        useCase.resolveConflict(id, CloudConflictResolution.KeepLocal) {
            assertEquals(CloudSyncOutcome.Verified(verified), it.getOrThrow())
        }
        assertEquals(CloudConflictResolution.KeepLocal, resolvedWith)
    }
}
