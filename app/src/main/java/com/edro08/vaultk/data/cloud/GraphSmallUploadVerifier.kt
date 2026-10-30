package com.edro08.vaultk.data.cloud

import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudCopyProtectedException
import com.edro08.vaultk.domain.cloud.CloudCopyState
import org.json.JSONObject
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Verifies the exact encrypted bytes after a trusted 2xx upload response or reconciles an
 * interrupted Pending attempt using GET only; never retries the PUT. */
internal class GraphSmallUploadVerifier(private val copies: PrivateCloudCopyStore,
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) {
    sealed interface Outcome {
        data class Confirmed(val copy: CloudCopy) : Outcome
        /** Includes remote changes, lost connectivity, invalid responses and failed local commit. */
        data object StillPending : Outcome
    }

    sealed interface ReconcileOutcome {
        data class Confirmed(val copy: CloudCopy) : ReconcileOutcome
        data class Rearmed(val copy: CloudCopy) : ReconcileOutcome
        data object Conflict : ReconcileOutcome
        data object Missing : ReconcileOutcome
        data object StillPending : ReconcileOutcome
    }

    sealed interface PreUploadOutcome {
        data object ReadyToUpload : PreUploadOutcome
        data object Conflict : PreUploadOutcome
        data object Missing : PreUploadOutcome
        data object StillPending : PreUploadOutcome
    }

    sealed interface RefreshOutcome {
        data class UpToDate(val copy: CloudCopy) : RefreshOutcome
        data class Downloaded(val copy: CloudCopy) : RefreshOutcome
        data class Confirmed(val copy: CloudCopy) : RefreshOutcome
        data class Rearmed(val copy: CloudCopy) : RefreshOutcome
        data object Conflict : RefreshOutcome
        data object Missing : RefreshOutcome
        data object StillPending : RefreshOutcome
        data object SyncError : RefreshOutcome
    }

    fun verify(token: String, authenticatedAccountId: String, id: CloudCopyId,
        receipt: GraphSmallPendingUploader.Outcome.AwaitingVerification): Outcome {
        require(token.isNotBlank() && id.accountId == authenticatedAccountId && receipt.itemId == id.itemId)
        return try {
            val verified = copies.withSmallPendingCiphertext(id) { pending, input ->
                if (pending.eTag == receipt.eTag) throw IOException("Invalid new ETag")
                val localHash = digest(input, pending.bytes)
                val path = "https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
                if (!metadataMatches(path, token, id, receipt.eTag, pending.bytes))
                    throw IOException("Remote item changed")
                val remoteHash = downloadHash(path, token, pending.bytes)
                if (!MessageDigest.isEqual(localHash, remoteHash) ||
                    !metadataMatches(path, token, id, receipt.eTag, pending.bytes))
                    throw IOException("Remote copy not verified")
                pending.eTag to localHash
            }
            Outcome.Confirmed(copies.completeVerifiedSmallUpload(id, verified.first,
                receipt.eTag, verified.second))
        } catch (_: Exception) {
            // Especially after a success response, uncertainty must never discard Pending.
            Outcome.StillPending
        }
    }

    fun reconcile(token: String, authenticatedAccountId: String, id: CloudCopyId): ReconcileOutcome {
        require(token.isNotBlank() && authenticatedAccountId.isNotBlank() && id.accountId == authenticatedAccountId)
        var networkEntered = false
        return try {
            val step = copies.withAttemptedPendingSnapshot(id) { snapshot ->
                networkEntered = true
                val pending = snapshot.copy
                val path = "https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
                when (val first = readMetadata(path, token, id)) {
                    MetadataResult.Missing -> Step.Done(ReconcileOutcome.Missing)
                    MetadataResult.Invalid -> Step.Done(ReconcileOutcome.StillPending)
                    is MetadataResult.Found -> {
                        if (first.eTag == pending.eTag) {
                            if (metadataMatches(path, token, id, first.eTag, first.bytes))
                                Step.Rearm(pending.eTag, snapshot.currentHash)
                            else Step.Done(ReconcileOutcome.StillPending)
                        } else if (first.bytes == pending.bytes || first.bytes == snapshot.attemptedBytes) {
                            val remoteHash = downloadHash(path, token, first.bytes)
                            if (!metadataMatches(path, token, id, first.eTag, first.bytes))
                                Step.Done(ReconcileOutcome.StillPending)
                            else if (first.bytes == pending.bytes &&
                                MessageDigest.isEqual(snapshot.currentHash, remoteHash))
                                Step.Confirm(pending.eTag, first.eTag, snapshot.currentHash)
                            else if (snapshot.attemptedBlob != snapshot.currentBlob &&
                                first.bytes == snapshot.attemptedBytes &&
                                MessageDigest.isEqual(snapshot.attemptedHash, remoteHash))
                                Step.AdvanceBase(pending.eTag, first.eTag, snapshot.attemptedBlob,
                                    snapshot.attemptedHash, snapshot.currentHash)
                            else Step.Done(ReconcileOutcome.Conflict)
                        } else {
                            if (metadataMatches(path, token, id, first.eTag, first.bytes))
                                Step.Done(ReconcileOutcome.Conflict)
                            else Step.Done(ReconcileOutcome.StillPending)
                        }
                    }
                }
            }
            when (step) {
                is Step.Done -> step.outcome
                is Step.Confirm -> ReconcileOutcome.Confirmed(
                    copies.completeVerifiedSmallUpload(id, step.baseEtag, step.newEtag, step.fingerprint))
                is Step.Rearm -> ReconcileOutcome.Rearmed(
                    copies.rearmUncommittedPendingUpload(id, step.baseEtag, step.fingerprint))
                is Step.AdvanceBase -> ReconcileOutcome.Rearmed(
                    copies.advanceAttemptedPendingBase(id, step.baseEtag, step.newEtag,
                        step.attemptedBlob, step.attemptedFingerprint, step.currentFingerprint))
            }
        } catch (error: CloudCopyProtectedException) {
            if (!networkEntered) throw error else ReconcileOutcome.StillPending
        } catch (_: Exception) {
            ReconcileOutcome.StillPending
        }
    }

    /** Consults remote ETag before claiming a one-shot upload attempt so an offline save or
     * a pre-existing remote change never locks uploadAttempted or sends a blind PUT. */
    fun checkBeforeUpload(token: String, authenticatedAccountId: String, id: CloudCopyId): PreUploadOutcome {
        require(token.isNotBlank() && authenticatedAccountId.isNotBlank() && id.accountId == authenticatedAccountId)
        val local = copies.readCopy(id) ?: throw IOException("Cloud copy unavailable")
        if (local.state != CloudCopyState.Pending || local.uploadAttempted)
            throw CloudCopyProtectedException()
        if (local.eTag.isBlank() || local.bytes !in 8..SMALL_UPLOAD_MAX_BYTES)
            throw IOException("Pending copy not eligible for small upload")
        return try {
            val path = "https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
            when (val first = readMetadata(path, token, id)) {
                MetadataResult.Missing -> PreUploadOutcome.Missing
                MetadataResult.Invalid -> PreUploadOutcome.StillPending
                is MetadataResult.Found -> {
                    if (first.eTag == local.eTag) PreUploadOutcome.ReadyToUpload
                    else if (metadataMatches(path, token, id, first.eTag, first.bytes)) PreUploadOutcome.Conflict
                    else PreUploadOutcome.StillPending
                }
            }
        } catch (_: Exception) {
            PreUploadOutcome.StillPending
        }
    }

    /** Bidirectional check using GET only: downloads remote changes only when the local copy is Clean,
     * and detects conflicts or reconciles attempted uploads when the local copy is Pending. */
    fun refresh(token: String, authenticatedAccountId: String, id: CloudCopyId): RefreshOutcome {
        require(token.isNotBlank() && authenticatedAccountId.isNotBlank() && id.accountId == authenticatedAccountId)
        val local = copies.readCopy(id) ?: throw IOException("Cloud copy unavailable")
        val path = "https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
        return when (local.state) {
            CloudCopyState.Conflict -> RefreshOutcome.Conflict
            CloudCopyState.Pending -> {
                if (local.uploadAttempted) {
                    when (val reconciled = reconcile(token, authenticatedAccountId, id)) {
                        is ReconcileOutcome.Confirmed -> RefreshOutcome.Confirmed(reconciled.copy)
                        is ReconcileOutcome.Rearmed -> RefreshOutcome.Rearmed(reconciled.copy)
                        ReconcileOutcome.Conflict -> RefreshOutcome.Conflict
                        ReconcileOutcome.Missing -> RefreshOutcome.Missing
                        ReconcileOutcome.StillPending -> RefreshOutcome.StillPending
                    }
                } else {
                    try {
                        when (val first = readMetadata(path, token, id)) {
                            MetadataResult.Missing -> RefreshOutcome.Missing
                            MetadataResult.Invalid -> RefreshOutcome.StillPending
                            is MetadataResult.Found -> {
                                if (first.eTag == local.eTag)
                                    RefreshOutcome.StillPending
                                else if (metadataMatches(path, token, id, first.eTag, first.bytes))
                                    RefreshOutcome.Conflict
                                else RefreshOutcome.StillPending
                            }
                        }
                    } catch (_: Exception) {
                        RefreshOutcome.StillPending
                    }
                }
            }
            CloudCopyState.Clean -> {
                try {
                    when (val first = readMetadata(path, token, id)) {
                        MetadataResult.Missing -> RefreshOutcome.Missing
                        MetadataResult.Invalid -> RefreshOutcome.SyncError
                        is MetadataResult.Found -> {
                            if (first.eTag == local.eTag && first.bytes == local.bytes) {
                                if (metadataMatches(path, token, id, first.eTag, first.bytes))
                                    RefreshOutcome.UpToDate(local)
                                else RefreshOutcome.SyncError
                            } else {
                                val stagedBlob = copies.stageRemoteCiphertext(id, first.bytes) { output ->
                                    downloadToStream(path, token, first.bytes, output)
                                }
                                var committed = false
                                try {
                                    if (!metadataMatches(path, token, id, first.eTag, first.bytes))
                                        RefreshOutcome.SyncError
                                    else {
                                        val updated = copies.commitStagedRemoteClean(
                                            id, local.eTag, first.eTag, first.bytes, stagedBlob, first.name)
                                        committed = true
                                        RefreshOutcome.Downloaded(updated)
                                    }
                                } finally {
                                    if (!committed) copies.discardStagedRemoteCiphertext(id, stagedBlob)
                                }
                            }
                        }
                    }
                } catch (_: CloudCopyProtectedException) {
                    RefreshOutcome.StillPending
                } catch (_: Exception) {
                    RefreshOutcome.SyncError
                }
            }
        }
    }

    /** Downloads and verifies the remote conflicting KDBX ciphertext without overwriting the local
     * Pending ciphertext, preserving both encrypted blobs on disk before recording Conflict. */
    fun prepareConflict(token: String, authenticatedAccountId: String, id: CloudCopyId): CloudCopy? {
        require(token.isNotBlank() && authenticatedAccountId.isNotBlank() && id.accountId == authenticatedAccountId)
        val local = copies.readCopy(id) ?: return null
        if (local.state != CloudCopyState.Pending && local.state != CloudCopyState.Conflict) return null
        val path = "https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
        return try {
            when (val first = readMetadata(path, token, id)) {
                is MetadataResult.Found -> {
                    if (first.eTag == local.eTag) null
                    else if (local.state == CloudCopyState.Conflict &&
                        local.conflict?.remoteEtag == first.eTag &&
                        local.conflict.remoteBytes == first.bytes) {
                        local
                    } else {
                        val remoteBlob = copies.stageConflictCiphertext(id, first.bytes) { output ->
                            downloadToStream(path, token, first.bytes, output)
                        }
                        var recorded = false
                        try {
                            if (!metadataMatches(path, token, id, first.eTag, first.bytes)) null
                            else {
                                val updated = copies.recordConflict(
                                    id = id,
                                    expectedBaseEtag = local.eTag,
                                    remoteEtag = first.eTag,
                                    remoteBytes = first.bytes,
                                    remoteBlob = remoteBlob
                                )
                                recorded = true
                                updated
                            }
                        } finally {
                            if (!recorded) copies.discardStagedRemoteCiphertext(id, remoteBlob)
                        }
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadHash(path: String, token: String, expectedBytes: Long): ByteArray =
        withVerifiedDownload(path, token) { input -> digest(input, expectedBytes) }

    private fun downloadToStream(path: String, token: String, expectedBytes: Long, output: OutputStream) =
        withVerifiedDownload(path, token) { input ->
            val buffer = ByteArray(64 * 1024)
            var remaining = expectedBytes
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                if (count <= 0) throw IOException("Ciphertext shorter than expected")
                output.write(buffer, 0, count)
                remaining -= count
            }
            if (input.read() != -1) throw IOException("Ciphertext longer than expected")
        }

    private fun <T> withVerifiedDownload(path: String, token: String, consume: (InputStream) -> T): T {
        val redirect = request(URL("$path/content"), token)
        val location = try {
            if (redirect.responseCode !in 301..399) throw IOException("No download redirect")
            redirect.getHeaderField("Location") ?: throw IOException("No download URL")
        } finally { redirect.disconnect() }
        val url = URL(location)
        val uri = url.toURI()
        if (url.protocol != "https" || url.host.isBlank() || url.port !in listOf(-1, 443) ||
            uri.userInfo != null || uri.fragment != null) throw IOException("Unsafe download URL")
        val download = request(url, null)
        return try {
            if (download.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Download failed")
            download.inputStream.use { consume(it) }
        } finally { download.disconnect() }
    }

    private sealed interface MetadataResult {
        data class Found(val eTag: String, val bytes: Long, val name: String? = null) : MetadataResult
        data object Missing : MetadataResult
        data object Invalid : MetadataResult
    }

    private sealed interface Step {
        data class Done(val outcome: ReconcileOutcome) : Step
        data class Confirm(val baseEtag: String, val newEtag: String, val fingerprint: ByteArray) : Step
        data class Rearm(val baseEtag: String, val fingerprint: ByteArray) : Step
        data class AdvanceBase(val baseEtag: String, val newEtag: String, val attemptedBlob: String,
            val attemptedFingerprint: ByteArray, val currentFingerprint: ByteArray) : Step
    }

    private fun readMetadata(path: String, token: String, id: CloudCopyId): MetadataResult {
        val connection = request(URL("$path?\$select=id,name,eTag,size,parentReference,file"), token)
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return MetadataResult.Missing
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return MetadataResult.Invalid
            val bytes = connection.inputStream.use { it.readNBytes(MAX_RESPONSE_BYTES + 1) }
            if (bytes.size > MAX_RESPONSE_BYTES) return MetadataResult.Invalid
            val item = JSONObject(String(bytes, Charsets.UTF_8))
            val eTag = item.optString("eTag")
            val size = item.optLong("size", -1)
            val name = item.optString("name").takeIf { it.isNotBlank() }
            return if (item.optString("id") == id.itemId && eTag.isNotBlank() && size in 8..MAX_DOWNLOAD_BYTES &&
                (name == null || name.endsWith(".kdbx", ignoreCase = true)) &&
                item.optJSONObject("file") != null &&
                item.optJSONObject("parentReference")?.optString("driveId") == id.driveId)
                MetadataResult.Found(eTag, size, name)
            else MetadataResult.Invalid
        } finally { connection.disconnect() }
    }

    private fun metadataMatches(path: String, token: String, id: CloudCopyId,
        expectedEtag: String, expectedBytes: Long): Boolean =
        when (val current = readMetadata(path, token, id)) {
            is MetadataResult.Found -> current.eTag == expectedEtag && current.bytes == expectedBytes
            else -> false
        }

    private fun digest(input: InputStream, expectedBytes: Long): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var remaining = expectedBytes
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (count <= 0) throw IOException("Ciphertext shorter than expected")
            hash.update(buffer, 0, count)
            remaining -= count
        }
        if (input.read() != -1) throw IOException("Ciphertext longer than expected")
        return hash.digest()
    }

    private fun request(url: URL, token: String?): HttpURLConnection {
        if (token != null && (url.protocol != "https" || url.host != "graph.microsoft.com" ||
                url.port !in listOf(-1, 443))) throw IOException("Invalid Graph URL")
        val connection = open(url)
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return connection
    }

    private fun segment(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
    private companion object {
        const val MAX_RESPONSE_BYTES = 1_000_000
        const val MAX_DOWNLOAD_BYTES = 512L * 1024 * 1024
        const val SMALL_UPLOAD_MAX_BYTES = 250_000_000L
    }
}
