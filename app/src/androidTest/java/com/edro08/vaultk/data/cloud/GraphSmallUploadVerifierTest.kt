package com.edro08.vaultk.data.cloud

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudCopyState
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import com.edro08.vaultk.data.cloud.GraphSmallPendingUploader
import com.edro08.vaultk.data.cloud.GraphSmallUploadVerifier
import com.edro08.vaultk.data.cloud.PrivateCloudCopyStore
import com.edro08.vaultk.domain.cloud.CloudCopyProtectedException
import com.edro08.vaultk.domain.vault.VaultConflictException

/** All Graph responses are faked and private files are synthetic in an isolated test directory. */
class GraphSmallUploadVerifierTest {
    private val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "test-verify-upload-${UUID.randomUUID()}")
    private val store = PrivateCloudCopyStore(root)
    private val id = CloudCopyId("account", "drive", "item")
    private val bytes = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)
    private val receipt = GraphSmallPendingUploader.Outcome.AwaitingVerification("item", "new-etag")

    @After fun cleanup() { root.deleteRecursively() }

    private fun pending() {
        store.promote(CloudCopy(id, "synthetic.kdbx", "old-etag", 12L,
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        val metadata = root.walkTopDown().first { it.name == "record.json" }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Pending").toString())
        store.claimSmallPendingUpload(id)
    }

    private fun graph(remote: ByteArray = bytes, secondTag: String = "new-etag",
        redirect: String = "https://cdn.example.test/signed"): Pair<GraphSmallUploadVerifier, List<Fake>> {
        val opened = mutableListOf<Fake>()
        var metadataCalls = 0
        val verifier = GraphSmallUploadVerifier(store) { url ->
            val response = when {
                url.host == "cdn.example.test" -> Fake(url, 200, remote)
                url.path.endsWith("/content") -> Fake(url, 302, location = redirect)
                url.query?.contains("\$select") == true -> {
                    metadataCalls++
                    val tag = if (metadataCalls == 1) "new-etag" else secondTag
                    Fake(url, 200, """{"id":"item","eTag":"$tag","size":12,
                        "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                }
                else -> error("Unexpected request")
            }
            opened += response
            response
        }
        return verifier to opened
    }

    @Test fun confirmsOnlyAfterBothMetadataReadsAndMatchingEncryptedBytes() {
        pending()
        val (verifier, opened) = graph()
        val result = verifier.verify("test-token", id.accountId, id, receipt)
        assertEquals(GraphSmallUploadVerifier.Outcome.Confirmed(store.list().single()), result)
        assertEquals(CloudCopyState.Clean, store.list().single().state)
        assertEquals("new-etag", PrivateCloudCopyStore(root).list().single().eTag)
        assertEquals(4, opened.size)
        assertArrayEquals(bytes, root.walkTopDown().first { it.extension == "kdbx" }.readBytes())
        assertEquals(null, opened[2].getRequestProperty("Authorization"))
        assertEquals("Bearer test-token", opened[0].getRequestProperty("Authorization"))
        assertFalse(opened.any { it.instanceFollowRedirects })
    }

    @Test fun changedRemoteTagOrDifferentCiphertextKeepsPending() {
        pending()
        val (changed, _) = graph(secondTag = "someone-else")
        assertEquals(GraphSmallUploadVerifier.Outcome.StillPending,
            changed.verify("test-token", id.accountId, id, receipt))
        assertEquals(CloudCopyState.Pending, store.list().single().state)
        val (different, _) = graph(remote = "other content".toByteArray())
        assertEquals(GraphSmallUploadVerifier.Outcome.StillPending,
            different.verify("test-token", id.accountId, id, receipt))
        assertEquals("old-etag", store.list().single().eTag)
    }

    @Test fun unsafeRedirectCannotReceiveBearerOrMarkClean() {
        pending()
        val (verifier, opened) = graph(redirect = "http://evil.invalid/bytes")
        assertEquals(GraphSmallUploadVerifier.Outcome.StillPending,
            verifier.verify("test-token", id.accountId, id, receipt))
        assertEquals(2, opened.size)
        assertEquals(CloudCopyState.Pending, store.list().single().state)
    }

    @Test fun completionRejectsWrongBaseOrDifferentLocalCiphertext() {
        pending()
        assertThrows(com.edro08.vaultk.domain.cloud.CloudCopyProtectedException::class.java) {
            store.completeVerifiedSmallUpload(id, "wrong-base", "new-etag", ByteArray(32))
        }
        assertThrows(com.edro08.vaultk.domain.vault.VaultConflictException::class.java) {
            store.completeVerifiedSmallUpload(id, "old-etag", "new-etag", ByteArray(32))
        }
        assertEquals(CloudCopyState.Pending, PrivateCloudCopyStore(root).list().single().state)
    }

    @Test fun reconcilesCommittedRemoteUploadWithoutResendingPut() {
        pending()
        val (verifier, opened) = graph()
        val reconciled = verifier.reconcile("test-token", id.accountId, id)
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Confirmed(store.list().single()), reconciled)
        val persisted = PrivateCloudCopyStore(root).list().single()
        assertEquals(CloudCopyState.Clean, persisted.state)
        assertEquals("new-etag", persisted.eTag)
        assertFalse(persisted.uploadAttempted)
        assertTrue(opened.all { it.requestMethod == "GET" })
        assertEquals(null, opened[2].getRequestProperty("Authorization"))
    }

    @Test fun reconcilesUncommittedAttemptAtBaseEtagAndAllowsExplicitRetry() {
        pending()
        var metadataCalls = 0
        val reconcileOpened = mutableListOf<Fake>()
        val verifier = GraphSmallUploadVerifier(store) { url ->
            metadataCalls++
            val tag = if (metadataCalls == 2) "changed-mid-check" else "old-etag"
            Fake(url, 200, """{"id":"item","eTag":"$tag","size":12,
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                .also { reconcileOpened += it }
        }
        // An inconsistent second metadata read must leave uploadAttempted blocked.
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.StillPending,
            verifier.reconcile("test-token", id.accountId, id))
        assertTrue(PrivateCloudCopyStore(root).list().single().uploadAttempted)
        assertThrows(com.edro08.vaultk.domain.cloud.CloudCopyProtectedException::class.java) {
            GraphSmallPendingUploader(store) { Fake(it, 200) }.upload("test-token", id.accountId, id)
        }

        // Two consistent metadata reads at base ETag rearm the Pending copy without sending PUT.
        val stable = GraphSmallUploadVerifier(store) { url ->
            Fake(url, 200, """{"id":"item","eTag":"old-etag","size":12,
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                .also { reconcileOpened += it }
        }
        val rearmed = stable.reconcile("test-token", id.accountId, id)
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Rearmed(store.list().single()), rearmed)
        assertFalse(PrivateCloudCopyStore(root).list().single().uploadAttempted)
        assertTrue(reconcileOpened.all { it.requestMethod == "GET" })

        val putReply = """{"id":"item","eTag":"new-etag","size":12,
            "parentReference":{"driveId":"drive"}}""".toByteArray()
        val uploaded = GraphSmallPendingUploader(store) { url -> Fake(url, 200, putReply) }
            .upload("test-token", id.accountId, id)
        assertEquals(receipt, uploaded)
        assertEquals(GraphSmallUploadVerifier.Outcome.Confirmed(
            store.list().single().copy(eTag = "new-etag", state = CloudCopyState.Clean,
                savedAt = store.list().single().savedAt, uploadAttempted = false)
        ).javaClass, graph().first.verify("test-token", id.accountId, id, receipt).javaClass)
        assertEquals(CloudCopyState.Clean, PrivateCloudCopyStore(root).list().single().state)
    }

    @Test fun reconcilesConflictMissingAndCutsWithoutDiscardingBlobsOrRearming() {
        pending()
        // Same byte length as Pending, but different encrypted bytes on OneDrive -> Conflict.
        val (sameSizeConflict, _) = graph(remote = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 9, 9, 9, 9))
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Conflict,
            sameSizeConflict.reconcile("test-token", id.accountId, id))
        assertTrue(PrivateCloudCopyStore(root).list().single().uploadAttempted)

        // Different remote byte length -> Conflict after two metadata reads without downloading.
        val differentSizeOpened = mutableListOf<Fake>()
        val differentSize = GraphSmallUploadVerifier(store) { url ->
            Fake(url, 200, """{"id":"item","eTag":"remote-c","size":99,
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                .also { differentSizeOpened += it }
        }
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Conflict,
            differentSize.reconcile("test-token", id.accountId, id))
        assertEquals(2, differentSizeOpened.size)
        assertTrue(PrivateCloudCopyStore(root).list().single().uploadAttempted)

        // Remote 404 -> Missing, still Pending and still blocked from blind retry.
        val missing = GraphSmallUploadVerifier(store) { url -> Fake(url, 404) }
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Missing,
            missing.reconcile("test-token", id.accountId, id))
        assertTrue(PrivateCloudCopyStore(root).list().single().uploadAttempted)

        // Truncated download or network cut at each step -> StillPending, never Conflict or Clean.
        val truncated = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, bytes.copyOf(6))
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"item","eTag":"new-etag","size":12,
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.StillPending,
            truncated.reconcile("test-token", id.accountId, id))
        assertEquals(CloudCopyState.Pending, PrivateCloudCopyStore(root).list().single().state)
        assertTrue(PrivateCloudCopyStore(root).list().single().uploadAttempted)
    }

    @Test fun reconcilesSubsequentLocalEditAfterCommittedAttemptByAdvancingBaseEtag() {
        val key = "test-only".toCharArray()
        val initial = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("V0") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials(String(key).toByteArray()), out)
        }.toByteArray()
        val kdbxId = CloudCopyId("account", "drive", "kdbx-item")
        store.promote(CloudCopy(kdbxId, "synthetic.kdbx", "etag-0", initial.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(initial))
        val opened0 = store.openVersioned(kdbxId, key)
        val firstEdit = opened0.content.copy(root = opened0.content.root.copy(
            entries = opened0.content.root.entries.map { it.copy(title = "V1") }))
        val saved1 = store.savePending(kdbxId, opened0.fingerprint, key, opened0.content, firstEdit)
        store.claimSmallPendingUpload(kdbxId)
        val attemptedRecord = root.walkTopDown().first {
            it.name == "record.json" && JSONObject(it.readText()).getString("itemId") == "kdbx-item"
        }
        val attemptedBlobFile = File(attemptedRecord.parentFile, JSONObject(attemptedRecord.readText()).getString("blob"))
        val attemptedBytes = attemptedBlobFile.readBytes()

        // User edits again offline while the first upload outcome is still unresolved.
        val secondEdit = saved1.content.copy(root = saved1.content.root.copy(
            entries = saved1.content.root.entries.map { it.copy(title = "V2") }))
        store.savePending(kdbxId, saved1.fingerprint, key, saved1.content, secondEdit)
        assertTrue(store.list().first { it.id == kdbxId }.uploadAttempted)

        // OneDrive actually committed V1 as etag-1; reconcile advances base ETag to etag-1 and rearms V2.
        val verifier = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, attemptedBytes)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"kdbx-item","eTag":"etag-1","size":${attemptedBytes.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }
        val reconciled = verifier.reconcile("test-token", kdbxId.accountId, kdbxId)
        val updated = PrivateCloudCopyStore(root).list().first { it.id == kdbxId }
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Rearmed(updated), reconciled)
        assertEquals(CloudCopyState.Pending, updated.state)
        assertEquals("etag-1", updated.eTag)
        assertFalse(updated.uploadAttempted)
        assertEquals("V2", store.open(kdbxId, key).root.entries.single().title)
        // All 3 encrypted snapshots (Clean V0, attempted V1, current Pending V2) remain on disk.
        assertEquals(3, attemptedRecord.parentFile!!.listFiles()!!.count { it.extension == "kdbx" })
    }

    @Test fun reconcileRejectsIneligibleCopyOrAccountBeforeOpeningNetwork() {
        var opened = 0
        val verifier = GraphSmallUploadVerifier(store) { url -> opened++; Fake(url, 200) }
        store.promote(CloudCopy(id, "clean.kdbx", "v1", 12L, 1L, CloudCopyState.Clean),
            ByteArrayInputStream(bytes))
        assertThrows(IllegalArgumentException::class.java) {
            verifier.reconcile("test-token", "other-account", id)
        }
        assertThrows(com.edro08.vaultk.domain.cloud.CloudCopyProtectedException::class.java) {
            verifier.reconcile("test-token", id.accountId, id)
        }
        val metadata = root.walkTopDown().first { it.name == "record.json" }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Pending").toString())
        assertThrows(com.edro08.vaultk.domain.cloud.CloudCopyProtectedException::class.java) {
            verifier.reconcile("test-token", id.accountId, id)
        }
        assertEquals(0, opened)
    }

    @Test fun cutsAtEveryStepOfUploadVerifyAndReconcilePreservePendingAndRecoverAcrossRestarts() {
        val cutId = CloudCopyId("account", "drive", "cut-item")
        store.promote(CloudCopy(cutId, "synthetic.kdbx", "old-etag", 12L,
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        val metadata = root.walkTopDown().first {
            it.name == "record.json" && JSONObject(it.readText()).getString("itemId") == "cut-item"
        }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Pending").toString())

        // 1. Cut during PUT request -> durable uploadAttempted across store restart.
        assertEquals(GraphSmallPendingUploader.Outcome.Uncertain,
            GraphSmallPendingUploader(store) { throw IOException("Network cut during PUT") }
                .upload("test-token", cutId.accountId, cutId))
        val afterPutCut = PrivateCloudCopyStore(root)
        assertTrue(afterPutCut.list().first { it.id == cutId }.uploadAttempted)
        assertThrows(com.edro08.vaultk.domain.cloud.CloudCopyProtectedException::class.java) {
            GraphSmallPendingUploader(afterPutCut) { Fake(it, 200) }.upload("test-token", cutId.accountId, cutId)
        }

        // 2. Cuts at each step of verify (metadata 1, redirect, download, metadata 2).
        val candidate = GraphSmallPendingUploader.Outcome.AwaitingVerification("cut-item", "new-etag")
        for (failStep in 1..4) {
            var step = 0
            val interruptedVerifier = GraphSmallUploadVerifier(PrivateCloudCopyStore(root)) { url ->
                step++
                if (step == failStep) throw IOException("Cut at verify step $failStep")
                when {
                    url.host == "cdn.example.test" -> Fake(url, 200, bytes)
                    url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                    else -> Fake(url, 200, """{"id":"cut-item","eTag":"new-etag","size":12,
                        "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                }
            }
            assertEquals(GraphSmallUploadVerifier.Outcome.StillPending,
                interruptedVerifier.verify("test-token", cutId.accountId, cutId, candidate))
            val restarted = PrivateCloudCopyStore(root).list().first { it.id == cutId }
            assertEquals(CloudCopyState.Pending, restarted.state)
            assertEquals("old-etag", restarted.eTag)
            assertTrue(restarted.uploadAttempted)
        }

        // 3. Cuts at each step of reconcile (metadata 1, redirect, download, metadata 2).
        for (failStep in 1..4) {
            var step = 0
            val interruptedReconciler = GraphSmallUploadVerifier(PrivateCloudCopyStore(root)) { url ->
                step++
                if (step == failStep) throw IOException("Cut at reconcile step $failStep")
                when {
                    url.host == "cdn.example.test" -> Fake(url, 200, bytes)
                    url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                    else -> Fake(url, 200, """{"id":"cut-item","eTag":"new-etag","size":12,
                        "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                }
            }
            assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.StillPending,
                interruptedReconciler.reconcile("test-token", cutId.accountId, cutId))
            val restarted = PrivateCloudCopyStore(root).list().first { it.id == cutId }
            assertEquals(CloudCopyState.Pending, restarted.state)
            assertEquals("old-etag", restarted.eTag)
            assertTrue(restarted.uploadAttempted)
        }

        // 4. Final reconcile after restart confirms the remotely committed ciphertext without PUT.
        val recovered = GraphSmallUploadVerifier(PrivateCloudCopyStore(root)) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, bytes)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"cut-item","eTag":"new-etag","size":12,
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }.reconcile("test-token", cutId.accountId, cutId)
        val clean = PrivateCloudCopyStore(root).list().first { it.id == cutId }
        assertEquals(GraphSmallUploadVerifier.ReconcileOutcome.Confirmed(clean), recovered)
        assertEquals(CloudCopyState.Clean, clean.state)
        assertEquals("new-etag", clean.eTag)
        assertFalse(clean.uploadAttempted)
    }

    @Test fun refreshDownloadsRemoteUpdateOnlyWhenCopyIsCleanAndPreservesPriorBlob() {
        val cleanId = id.copy(itemId = "clean-refresh")
        store.promote(CloudCopy(cleanId, "synthetic.kdbx", "v1", bytes.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        val remoteUpdated = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 9, 8, 7, 6, 5, 4)

        // 1. Same remote ETag returns UpToDate without downloading /content.
        val upToDateRequests = mutableListOf<Fake>()
        val upToDate = GraphSmallUploadVerifier(store) { url ->
            Fake(url, 200, """{"id":"clean-refresh","name":"synthetic.kdbx","eTag":"v1","size":12,
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray()).also { upToDateRequests += it }
        }.refresh("test-token", cleanId.accountId, cleanId)
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.UpToDate(store.list().first { it.id == cleanId }), upToDate)
        assertEquals(2, upToDateRequests.size)
        assertFalse(upToDateRequests.any { it.url.path.endsWith("/content") })

        // 2. If second metadata read after download disagrees, staged blob is discarded and v1 stays intact.
        var metaCalls = 0
        val aborted = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, remoteUpdated)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> {
                    metaCalls++
                    val tag = if (metaCalls == 1) "v2" else "v3"
                    Fake(url, 200, """{"id":"clean-refresh","name":"synthetic.kdbx","eTag":"$tag","size":${remoteUpdated.size},
                        "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
                }
            }
        }.refresh("test-token", cleanId.accountId, cleanId)
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.SyncError, aborted)
        assertEquals("v1", store.list().first { it.id == cleanId }.eTag)
        assertEquals(1, root.walkTopDown().count { it.extension == "kdbx" })

        // 3. Consistent new remote ETag downloads ciphertext without bearer to CDN, promotes v2 and keeps v1 blob.
        val downloadRequests = mutableListOf<Fake>()
        val downloaded = GraphSmallUploadVerifier(store) { url ->
            val response = when {
                url.host == "cdn.example.test" -> Fake(url, 200, remoteUpdated)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"clean-refresh","name":"synthetic.kdbx","eTag":"v2","size":${remoteUpdated.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
            downloadRequests += response
            response
        }.refresh("test-token", cleanId.accountId, cleanId)
        val persisted = PrivateCloudCopyStore(root).list().first { it.id == cleanId }
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.Downloaded(persisted), downloaded)
        assertEquals(CloudCopyState.Clean, persisted.state)
        assertEquals("v2", persisted.eTag)
        assertEquals(remoteUpdated.size.toLong(), persisted.bytes)
        assertEquals(null, downloadRequests.first { it.url.host == "cdn.example.test" }.getRequestProperty("Authorization"))
        assertEquals(2, root.walkTopDown().count { it.extension == "kdbx" })
    }

    @Test fun refreshAndCheckBeforeUploadNeverOverwritePendingCopyAndDetectConflictBeforePut() {
        val pendingId = id.copy(itemId = "pending-bidirectional")
        val initialBytes = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("BaseEntry") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("test".toByteArray()), out)
        }.toByteArray()
        store.promote(CloudCopy(pendingId, "synthetic.kdbx", "base-v1", initialBytes.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(initialBytes))
        val opened = store.openVersioned(pendingId, "test".toCharArray())
        val edited = opened.content.copy(root = opened.content.root.copy(
            entries = opened.content.root.entries.map { it.copy(title = "LocalPendingEntry", notes = "extra-notes") }))
        store.savePending(pendingId, opened.fingerprint, "test".toCharArray(), opened.content, edited)

        // 1. While Pending (!uploadAttempted) and remote is still at base-v1 (even with different size),
        // checkBeforeUpload returns ReadyToUpload and refresh returns StillPending without downloading.
        val readyVerifier = GraphSmallUploadVerifier(store) { url ->
            Fake(url, 200, """{"id":"pending-bidirectional","eTag":"base-v1","size":${initialBytes.size},
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
        }
        assertEquals(GraphSmallUploadVerifier.PreUploadOutcome.ReadyToUpload,
            readyVerifier.checkBeforeUpload("test-token", pendingId.accountId, pendingId))
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.StillPending,
            readyVerifier.refresh("test-token", pendingId.accountId, pendingId))
        assertFalse(store.list().first { it.id == pendingId }.uploadAttempted)

        // 2. Offline failure in checkBeforeUpload leaves uploadAttempted = false.
        val offlineVerifier = GraphSmallUploadVerifier(store) { throw IOException("Offline") }
        assertEquals(GraphSmallUploadVerifier.PreUploadOutcome.StillPending,
            offlineVerifier.checkBeforeUpload("test-token", pendingId.accountId, pendingId))
        assertFalse(store.list().first { it.id == pendingId }.uploadAttempted)

        // 3. Remote ETag change while Pending returns Conflict in both refresh and checkBeforeUpload
        // without downloading /content or setting uploadAttempted = true.
        val conflictRequests = mutableListOf<Fake>()
        val conflictVerifier = GraphSmallUploadVerifier(store) { url ->
            Fake(url, 200, """{"id":"pending-bidirectional","eTag":"remote-v2","size":${initialBytes.size + 64},
                "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray()).also { conflictRequests += it }
        }
        assertEquals(GraphSmallUploadVerifier.PreUploadOutcome.Conflict,
            conflictVerifier.checkBeforeUpload("test-token", pendingId.accountId, pendingId))
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.Conflict,
            conflictVerifier.refresh("test-token", pendingId.accountId, pendingId))
        assertFalse(conflictRequests.any { it.url.path.endsWith("/content") })
        val afterConflict = PrivateCloudCopyStore(root).list().first { it.id == pendingId }
        assertEquals(CloudCopyState.Pending, afterConflict.state)
        assertFalse(afterConflict.uploadAttempted)
        val reopened = store.open(pendingId, "test".toCharArray())
        assertTrue(reopened.root.entries.any { it.title == "LocalPendingEntry" })
    }

    @Test fun prepareConflictPreservesBothEncryptedKdbxBlobsAndSupportsExplicitResolutionAndSecretFreeLogs() {
        val conflictId = id.copy(itemId = "conflict-resolution")
        val secretKey = "SuperSecretKey#2026".toCharArray()
        val baseBytes = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("BaseTitle") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("SuperSecretKey#2026".toByteArray()), out)
        }.toByteArray()
        val remoteConflictBytes = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("RemoteConflictTitle") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("SuperSecretKey#2026".toByteArray()), out)
        }.toByteArray()

        store.promote(CloudCopy(conflictId, "synthetic.kdbx", "etag-base", baseBytes.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(baseBytes))
        val opened = store.openVersioned(conflictId, secretKey)
        val edited = opened.content.copy(root = opened.content.root.copy(
            entries = opened.content.root.entries.map { it.copy(title = "LocalConflictTitle", notes = "local-notes") }))
        store.savePending(conflictId, opened.fingerprint, secretKey, opened.content, edited)

        // 1. prepareConflict downloads and verifies the remote conflicting KDBX without overwriting the local Pending KDBX.
        val prepared = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, remoteConflictBytes)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"conflict-resolution","eTag":"etag-remote","size":${remoteConflictBytes.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }.prepareConflict("BearerSecretToken123", conflictId.accountId, conflictId)!!

        assertEquals(CloudCopyState.Conflict, prepared.state)
        assertEquals("etag-base", prepared.conflict!!.baseEtag)
        assertEquals("etag-remote", prepared.conflict!!.remoteEtag)
        assertEquals(remoteConflictBytes.size.toLong(), prepared.conflict!!.remoteBytes)
        // Base + LocalPending + RemoteConflict = 3 encrypted KDBX files on disk (1 active + 2 recovery snapshots).
        assertEquals(2, store.recoverySnapshots(conflictId))
        // While in Conflict, opening the copy still opens the local Pending KDBX.
        assertEquals("LocalConflictTitle", store.open(conflictId, secretKey).root.entries.single().title)

        // 2. Explicit UseRemote resolution switches active pointer to the verified remote KDBX while keeping local KDBX as recovery snapshot.
        val resolvedRemote = store.resolveConflictUseRemote(conflictId, "etag-base", "etag-remote")
        assertEquals(CloudCopyState.Clean, resolvedRemote.state)
        assertEquals("etag-remote", resolvedRemote.eTag)
        assertEquals("RemoteConflictTitle", store.open(conflictId, secretKey).root.entries.single().title)
        assertEquals(2, store.recoverySnapshots(conflictId))

        // 3. Re-enter Pending + Conflict and test explicit KeepLocal resolution.
        val reopenedRemote = store.openVersioned(conflictId, secretKey)
        val editedAgain = reopenedRemote.content.copy(root = reopenedRemote.content.root.copy(
            entries = reopenedRemote.content.root.entries.map { it.copy(title = "LocalKeptTitle") }))
        store.savePending(conflictId, reopenedRemote.fingerprint, secretKey, reopenedRemote.content, editedAgain)
        val secondPrepared = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, baseBytes)
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"conflict-resolution","eTag":"etag-remote-2","size":${baseBytes.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }.prepareConflict("BearerSecretToken123", conflictId.accountId, conflictId)!!
        assertEquals(CloudCopyState.Conflict, secondPrepared.state)

        val resolvedLocal = store.resolveConflictKeepLocal(conflictId, "etag-remote", "etag-remote-2")
        assertEquals(CloudCopyState.Pending, resolvedLocal.state)
        assertEquals("etag-remote-2", resolvedLocal.eTag)
        assertFalse(resolvedLocal.uploadAttempted)
        assertEquals("LocalKeptTitle", store.open(conflictId, secretKey).root.entries.single().title)
        assertEquals(4, store.recoverySnapshots(conflictId))

        // 4. Operation logs contain actions and metadata without secrets, tokens, entry titles, or file paths.
        val logs = store.logs(conflictId)
        assertTrue(logs.isNotEmpty())
        val rawLogText = File(root, "sync-log.json").readText()
        assertFalse(rawLogText.contains("SuperSecretKey"))
        assertFalse(rawLogText.contains("BearerSecretToken123"))
        assertFalse(rawLogText.contains("LocalConflictTitle"))
        assertFalse(rawLogText.contains("RemoteConflictTitle"))
        assertFalse(rawLogText.contains("LocalKeptTitle"))
        assertFalse(rawLogText.contains(root.absolutePath))
    }

    @Test fun expiredTokensRevokedPermissionsDeletedItemsConcurrentEditsAndTransferCutsNeverCorruptOrLoseData() {
        val hardenId = id.copy(itemId = "harden-item")
        val masterKey = "MasterSecret#2208".toCharArray()
        val cleanBytes = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("CleanHardenEntry") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("MasterSecret#2208".toByteArray()), out)
        }.toByteArray()
        val remoteBytes = ByteArrayOutputStream().also { out ->
            KdbxDatabase().apply { rootGroup.addEntry("RemoteHardenEntry") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("MasterSecret#2208".toByteArray()), out)
        }.toByteArray()

        store.promote(CloudCopy(hardenId, "synthetic.kdbx", "etag-v1", cleanBytes.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(cleanBytes))

        // 1. Expired token (401) and revoked permission (403) on Clean refresh keep local KDBX intact.
        for (status in listOf(401, 403)) {
            val outcome = GraphSmallUploadVerifier(store) { url ->
                Fake(url, status, """{"error":{"code":"unauthenticated"}}""".toByteArray())
            }.refresh("ExpiredTokenSecret", hardenId.accountId, hardenId)
            assertEquals(GraphSmallUploadVerifier.RefreshOutcome.SyncError, outcome)
            assertEquals("CleanHardenEntry", store.open(hardenId, masterKey).root.entries.single().title)
        }

        // 2. Deleted remote file (404) on Clean refresh reports Missing while local copy stays decryptable offline.
        val missingClean = GraphSmallUploadVerifier(store) { url -> Fake(url, 404) }
            .refresh("ValidTokenSecret", hardenId.accountId, hardenId)
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.Missing, missingClean)
        assertEquals("CleanHardenEntry", store.open(hardenId, masterKey).root.entries.single().title)

        // 3. Truncated or non-KDBX download during Clean refresh discards staged file and preserves Clean v1.
        val truncatedRefresh = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> Fake(url, 200, remoteBytes.copyOf(remoteBytes.size / 2))
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"harden-item","eTag":"etag-v2","size":${remoteBytes.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }.refresh("ValidTokenSecret", hardenId.accountId, hardenId)
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.SyncError, truncatedRefresh)
        assertEquals(0, store.recoverySnapshots(hardenId))

        // 4. Concurrent local edit (savePending) while remote download is in flight:
        // commitStagedRemoteClean refuses to overwrite the new Pending copy and discards the staged file.
        val concurrentRefresh = GraphSmallUploadVerifier(store) { url ->
            when {
                url.host == "cdn.example.test" -> {
                    val opened = store.openVersioned(hardenId, masterKey)
                    val localEdit = opened.content.copy(root = opened.content.root.copy(
                        entries = opened.content.root.entries.map { it.copy(title = "ConcurrentLocalEdit") }))
                    store.savePending(hardenId, opened.fingerprint, masterKey, opened.content, localEdit)
                    Fake(url, 200, remoteBytes)
                }
                url.path.endsWith("/content") -> Fake(url, 302, location = "https://cdn.example.test/signed")
                else -> Fake(url, 200, """{"id":"harden-item","eTag":"etag-v2","size":${remoteBytes.size},
                    "parentReference":{"driveId":"drive"},"file":{}}""".toByteArray())
            }
        }.refresh("ValidTokenSecret", hardenId.accountId, hardenId)
        assertEquals(GraphSmallUploadVerifier.RefreshOutcome.StillPending, concurrentRefresh)
        assertEquals(CloudCopyState.Pending, store.list().first { it.id == hardenId }.state)
        assertEquals("ConcurrentLocalEdit", store.open(hardenId, masterKey).root.entries.single().title)
        assertEquals(1, store.recoverySnapshots(hardenId)) // Only the prior Clean v1 snapshot; staged remote was discarded.

        // 5. Expired token (401), revoked permission (403), and deleted item (404) while Pending never lock uploadAttempted.
        for (status in listOf(401, 403)) {
            assertEquals(GraphSmallUploadVerifier.PreUploadOutcome.StillPending,
                GraphSmallUploadVerifier(store) { Fake(it, status) }
                    .checkBeforeUpload("ExpiredTokenSecret", hardenId.accountId, hardenId))
            assertFalse(store.list().first { it.id == hardenId }.uploadAttempted)
        }
        assertEquals(GraphSmallUploadVerifier.PreUploadOutcome.Missing,
            GraphSmallUploadVerifier(store) { Fake(it, 404) }
                .checkBeforeUpload("ValidTokenSecret", hardenId.accountId, hardenId))
        assertFalse(store.list().first { it.id == hardenId }.uploadAttempted)
        assertEquals("ConcurrentLocalEdit", store.open(hardenId, masterKey).root.entries.single().title)
    }

    private class Fake(url: URL, private val code: Int, private val bytes: ByteArray = byteArrayOf(),
        private val location: String? = null) : HttpURLConnection(url) {
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getResponseCode() = code
        override fun getInputStream() = ByteArrayInputStream(bytes)
        override fun getHeaderField(name: String) = if (name == "Location") location else null
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
    }
}
