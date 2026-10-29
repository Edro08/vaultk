package com.edro08.vaultk.data

import android.util.AtomicFile
import android.net.Uri
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyConflict
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyRepository
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudCopyProtectedException
import com.edro08.vaultk.domain.CloudSyncAction
import com.edro08.vaultk.domain.CloudSyncLogEntry
import com.edro08.vaultk.domain.VaultConflictException
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.OpenedVault
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

/** Private, durable encrypted snapshots. Never passes its file paths to the local vault writer. */
class PrivateCloudCopyStore(private val root: File, private val reader: KdbxReader = KdbxReader(),
    private val attachmentInput: (Uri) -> InputStream = { throw IOException("Attachment unavailable") }) : CloudCopyRepository {
    override fun list(): List<CloudCopy> = root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
        runCatching { read(dir)?.first }.getOrNull()
    }.sortedByDescending { it.savedAt }

    override fun selected(): CloudCopy? {
        val marker = AtomicFile(File(root, "active.json"))
        if (!marker.baseFile.exists() && !File(root, "active.json.bak").exists()) return null
        return runCatching {
            val bytes = marker.openRead().use { it.readNBytes(16_385) }
            if (bytes.size > 16_384) throw IOException("Invalid selection")
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            val id = CloudCopyId(json.getString("accountId"), json.getString("driveId"), json.getString("itemId"))
            read(directory(id))?.first
        }.getOrNull()
    }

    @Synchronized
    override fun select(id: CloudCopyId?) {
        val marker = AtomicFile(File(root, "active.json"))
        if (id == null) { marker.delete(); return }
        if (read(directory(id)) == null) throw IOException("Cloud copy unavailable")
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Private storage unavailable")
        val data = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).toString().toByteArray(Charsets.UTF_8)
        val output = marker.startWrite()
        try { output.write(data); marker.finishWrite(output) }
        catch (error: Exception) { marker.failWrite(output); throw error }
    }

    @Synchronized
    override fun open(id: CloudCopyId, key: CharArray): VaultContent = openVersioned(id, key).content

    @Synchronized
    override fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault {
        val record = read(directory(id)) ?: throw IOException("Cloud copy unavailable")
        val file = File(directory(id), record.second)
        if (file.length() != record.first.bytes) throw IOException("Cloud copy size mismatch")
        // Detect a concurrent replacement or damaged ciphertext without ever writing the file.
        val transaction = LocalVaultTransaction(reader)
        val before = transaction.fingerprint(file)
        val content = file.inputStream().use { reader.read(it, key) }
        if (!MessageDigest.isEqual(before, transaction.fingerprint(file))) throw VaultConflictException()
        return OpenedVault(content, before)
    }

    @Synchronized
    override fun savePending(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent): OpenedVault {
        val dir = directory(id)
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        if (record.first.state == CloudCopyState.Conflict) throw CloudCopyProtectedException()
        // Editing an already-attempted Pending copy must not silently rearm the network send.
        val previouslyAttempted = record.first.state == CloudCopyState.Pending && record.first.uploadAttempted
        val attempted = if (previouslyAttempted) attemptedBlob(dir, record.second) else null
        val previous = File(dir, record.second)
        val transaction = LocalVaultTransaction(reader)
        if (previous.length() != record.first.bytes ||
            !MessageDigest.isEqual(expectedFingerprint, transaction.fingerprint(previous)))
            throw VaultConflictException()
        var encrypted: ByteArray? = null
        var staged: File? = null
        var published = false
        try {
            val prepared = previous.inputStream().use { input ->
                KdbxWriter(reader).prepare(input, key, original, changed,
                    attachmentInput = attachmentInput)
            }
            encrypted = prepared
            if (prepared.size.toLong() !in 8..SMALL_UPLOAD_MAX_BYTES) throw IOException("Invalid replacement size")
            val file = File(dir, "${UUID.randomUUID()}.kdbx")
            staged = file
            file.outputStream().use { output ->
                output.write(prepared)
                output.fd.sync()
            }
            val fingerprint = transaction.fingerprint(file)
            if (!MessageDigest.isEqual(fingerprint, MessageDigest.getInstance("SHA-256").digest(prepared)))
                throw IOException("Staged copy differs")
            val verified = file.inputStream().use { reader.read(it, key) }
            if (read(dir) != record ||
                !MessageDigest.isEqual(expectedFingerprint, transaction.fingerprint(previous)))
                throw VaultConflictException()
            val pending = record.first.copy(bytes = file.length(), savedAt = System.currentTimeMillis(),
                state = CloudCopyState.Pending, uploadAttempted = previouslyAttempted)
            val data = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
                .put("itemId", id.itemId).put("name", pending.name).put("eTag", pending.eTag)
                .put("accountName", pending.accountName).put("bytes", pending.bytes)
                .put("savedAt", pending.savedAt).put("state", pending.state.name)
                .put("uploadAttempted", previouslyAttempted)
                .apply { if (attempted != null) put("attemptedBlob", attempted) }
                .put("blob", file.name).toString().toByteArray(Charsets.UTF_8)
            val metadata = AtomicFile(File(dir, "record.json"))
            val output = metadata.startWrite()
            try { output.write(data); metadata.finishWrite(output) }
            catch (error: Exception) { metadata.failWrite(output); throw error }
            published = true
            appendLog(id, CloudSyncAction.SavePending, CloudCopyState.Pending, pending.bytes, pending.eTag)
            return OpenedVault(verified, fingerprint)
        } finally {
            encrypted?.fill(0)
            staged?.let { file ->
                // On an ambiguous metadata write leave ciphertext for recovery rather than
                // risking deletion of the blob to which the record may already point.
                if (!published && runCatching {
                        read(dir)?.second?.let { it != file.name } ?: false
                    }.getOrDefault(false))
                    file.delete()
            }
        }
    }

    /** Holds the private Pending snapshot stable during one conditional upload attempt.
     * Never exposes its file path outside data or marks the copy Clean. */
    @Synchronized
    internal fun <T> withSmallPendingCiphertext(id: CloudCopyId,
        send: (CloudCopy, InputStream) -> T): T {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val copy = record.first
        if (copy.state != CloudCopyState.Pending) throw CloudCopyProtectedException()
        if (copy.eTag.isBlank() || copy.bytes !in 8..SMALL_UPLOAD_MAX_BYTES)
            throw IOException("Pending copy not eligible for small upload")
        val file = File(dir, record.second)
        if (Files.isSymbolicLink(file.toPath()) || file.length() != copy.bytes || !validSignature(file))
            throw IOException("Pending ciphertext unavailable")
        val before = LocalVaultTransaction(reader).fingerprint(file)
        val result = file.inputStream().use { send(copy, it) }
        if (read(dir) != record || file.length() != copy.bytes ||
            !MessageDigest.isEqual(before, LocalVaultTransaction(reader).fingerprint(file)))
            throw VaultConflictException()
        return result
    }

    /** Durable one-shot claim, written before a possible Graph request. An uncertain attempt
     * remains blocked after process death; it must be reconciled rather than retried blindly. */
    @Synchronized
    internal fun claimSmallPendingUpload(id: CloudCopyId) {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        if (record.first.state != CloudCopyState.Pending) throw CloudCopyProtectedException()
        val metadata = AtomicFile(File(dir, "record.json"))
        val content = metadata.openRead().use { it.readNBytes(65_537) }
        if (content.size > 65_536) throw IOException("Invalid cloud copy record")
        val value = JSONObject(String(content, Charsets.UTF_8))
        if (value.getString("blob") != record.second || value.optBoolean("uploadAttempted", false))
            throw CloudCopyProtectedException()
        // The existing snapshot validator also rejects over-limit, missing or damaged bytes.
        withSmallPendingCiphertext(id) { _, _ -> Unit }
        value.put("uploadAttempted", true)
        value.put("attemptedBlob", record.second)
        val output = metadata.startWrite()
        try { output.write(value.toString().toByteArray(Charsets.UTF_8)); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.UploadClaimed, CloudCopyState.Pending, record.first.bytes, record.first.eTag)
    }

    internal data class AttemptedPendingSnapshot(
        val copy: CloudCopy,
        val currentBlob: String,
        val currentHash: ByteArray,
        val attemptedBlob: String,
        val attemptedBytes: Long,
        val attemptedHash: ByteArray
    )

    @Synchronized
    internal fun <T> withAttemptedPendingSnapshot(id: CloudCopyId,
        inspect: (AttemptedPendingSnapshot) -> T): T {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val copy = record.first
        if (copy.state != CloudCopyState.Pending || !copy.uploadAttempted)
            throw CloudCopyProtectedException()
        if (copy.eTag.isBlank() || copy.bytes !in 8..SMALL_UPLOAD_MAX_BYTES)
            throw IOException("Pending copy not eligible for small upload")
        val currentFile = File(dir, record.second)
        if (Files.isSymbolicLink(currentFile.toPath()) || currentFile.length() != copy.bytes ||
            !validSignature(currentFile)) throw IOException("Pending ciphertext unavailable")
        val transaction = LocalVaultTransaction(reader)
        val currentHash = transaction.fingerprint(currentFile)
        val attemptedName = attemptedBlob(dir, record.second)
        val attemptedFile = File(dir, attemptedName)
        if (!attemptedName.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")) ||
            !attemptedFile.isFile || Files.isSymbolicLink(attemptedFile.toPath()))
            throw IOException("Attempted ciphertext unavailable")
        val attemptedBytes = attemptedFile.length()
        if (attemptedBytes !in 8..SMALL_UPLOAD_MAX_BYTES || !validSignature(attemptedFile))
            throw IOException("Attempted ciphertext unavailable")
        val attemptedHash = if (attemptedName == record.second) currentHash
            else transaction.fingerprint(attemptedFile)
        val result = inspect(AttemptedPendingSnapshot(copy, record.second, currentHash,
            attemptedName, attemptedBytes, attemptedHash))
        if (read(dir) != record || currentFile.length() != copy.bytes ||
            !MessageDigest.isEqual(currentHash, transaction.fingerprint(currentFile)) ||
            attemptedFile.length() != attemptedBytes ||
            !MessageDigest.isEqual(attemptedHash, transaction.fingerprint(attemptedFile)))
            throw VaultConflictException()
        return result
    }

    /** Commit only the same Pending ciphertext and base ETag that were remotely verified. */
    @Synchronized
    internal fun completeVerifiedSmallUpload(id: CloudCopyId, baseEtag: String, newEtag: String,
        expectedFingerprint: ByteArray): CloudCopy {
        require(baseEtag.isNotBlank() && newEtag.isNotBlank() && baseEtag != newEtag)
        require(expectedFingerprint.size == 32)
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val old = record.first
        if (old.state != CloudCopyState.Pending || old.eTag != baseEtag || !old.uploadAttempted)
            throw CloudCopyProtectedException()
        val file = File(dir, record.second)
        if (Files.isSymbolicLink(file.toPath()) || file.length() != old.bytes ||
            !MessageDigest.isEqual(expectedFingerprint, LocalVaultTransaction(reader).fingerprint(file)))
            throw VaultConflictException()
        val clean = old.copy(eTag = newEtag, state = CloudCopyState.Clean,
            savedAt = System.currentTimeMillis(), uploadAttempted = false)
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", clean.name).put("eTag", clean.eTag)
            .put("accountName", clean.accountName).put("bytes", clean.bytes)
            .put("savedAt", clean.savedAt).put("state", clean.state.name)
            .put("uploadAttempted", false)
            .put("blob", record.second).toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.UploadVerified, CloudCopyState.Clean, clean.bytes, "$baseEtag->$newEtag")
        return clean
    }

    /** Rearm only after remote metadata confirms the base ETag never changed on OneDrive. */
    @Synchronized
    internal fun rearmUncommittedPendingUpload(id: CloudCopyId, baseEtag: String,
        expectedFingerprint: ByteArray): CloudCopy {
        require(baseEtag.isNotBlank() && expectedFingerprint.size == 32)
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val old = record.first
        if (old.state != CloudCopyState.Pending || old.eTag != baseEtag || !old.uploadAttempted)
            throw CloudCopyProtectedException()
        val file = File(dir, record.second)
        if (Files.isSymbolicLink(file.toPath()) || file.length() != old.bytes ||
            !MessageDigest.isEqual(expectedFingerprint, LocalVaultTransaction(reader).fingerprint(file)))
            throw VaultConflictException()
        val rearmed = old.copy(uploadAttempted = false)
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", rearmed.name).put("eTag", rearmed.eTag)
            .put("accountName", rearmed.accountName).put("bytes", rearmed.bytes)
            .put("savedAt", rearmed.savedAt).put("state", rearmed.state.name)
            .put("uploadAttempted", false)
            .put("blob", record.second).toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.UploadRearmed, CloudCopyState.Pending, rearmed.bytes, baseEtag)
        return rearmed
    }

    /** If an earlier attempted Pending snapshot landed remotely and the user later edited locally,
     * advance the sync base ETag to that verified snapshot and rearm the newer local Pending blob. */
    @Synchronized
    internal fun advanceAttemptedPendingBase(id: CloudCopyId, baseEtag: String, newEtag: String,
        attemptedBlob: String, expectedAttemptedFingerprint: ByteArray,
        expectedCurrentFingerprint: ByteArray): CloudCopy {
        require(baseEtag.isNotBlank() && newEtag.isNotBlank() && baseEtag != newEtag)
        require(expectedAttemptedFingerprint.size == 32 && expectedCurrentFingerprint.size == 32)
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val old = record.first
        if (old.state != CloudCopyState.Pending || old.eTag != baseEtag || !old.uploadAttempted ||
            attemptedBlob == record.second || attemptedBlob(dir, record.second) != attemptedBlob)
            throw CloudCopyProtectedException()
        val transaction = LocalVaultTransaction(reader)
        val attemptedFile = File(dir, attemptedBlob)
        val currentFile = File(dir, record.second)
        if (!attemptedFile.isFile || Files.isSymbolicLink(attemptedFile.toPath()) ||
            Files.isSymbolicLink(currentFile.toPath()) || currentFile.length() != old.bytes ||
            !MessageDigest.isEqual(expectedAttemptedFingerprint, transaction.fingerprint(attemptedFile)) ||
            !MessageDigest.isEqual(expectedCurrentFingerprint, transaction.fingerprint(currentFile)))
            throw VaultConflictException()
        val updated = old.copy(eTag = newEtag, uploadAttempted = false)
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", updated.name).put("eTag", updated.eTag)
            .put("accountName", updated.accountName).put("bytes", updated.bytes)
            .put("savedAt", updated.savedAt).put("state", updated.state.name)
            .put("uploadAttempted", false)
            .put("blob", record.second).toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.BaseAdvanced, CloudCopyState.Pending, updated.bytes, "$baseEtag->$newEtag")
        return updated
    }

    @Synchronized
    internal fun readCopy(id: CloudCopyId): CloudCopy? {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) return null
        return read(dir)?.first
    }

    internal fun stageRemoteCiphertext(id: CloudCopyId, expectedBytes: Long,
        write: (OutputStream) -> Unit): String {
        require(expectedBytes in 8..MAX_BYTES)
        val dir = synchronized(this) {
            val target = directory(id)
            if (!target.isDirectory || Files.isSymbolicLink(target.toPath()))
                throw IOException("Cloud copy unavailable")
            val record = read(target) ?: throw IOException("Cloud copy unavailable")
            if (record.first.state != CloudCopyState.Clean) throw CloudCopyProtectedException()
            target
        }
        val blob = "${UUID.randomUUID()}.kdbx"
        val file = File(dir, blob)
        try {
            file.outputStream().use { out ->
                write(out)
                out.fd.sync()
            }
            if (file.length() != expectedBytes) throw IOException("Cloud copy size mismatch")
            if (!validSignature(file)) throw IOException("Invalid KDBX signature")
            return blob
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    @Synchronized
    internal fun commitStagedRemoteClean(id: CloudCopyId, expectedBaseEtag: String, newEtag: String,
        newBytes: Long, stagedBlob: String, newName: String? = null): CloudCopy {
        require(expectedBaseEtag.isNotBlank() && newEtag.isNotBlank() &&
            expectedBaseEtag != newEtag && newBytes in 8..MAX_BYTES)
        require(stagedBlob.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")))
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val stagedFile = File(dir, stagedBlob)
        val record = read(dir) ?: run {
            stagedFile.delete()
            throw IOException("Cloud copy unavailable")
        }
        val old = record.first
        if (old.state != CloudCopyState.Clean || old.eTag != expectedBaseEtag || record.second == stagedBlob) {
            if (record.second != stagedBlob) stagedFile.delete()
            throw CloudCopyProtectedException()
        }
        if (!stagedFile.isFile || Files.isSymbolicLink(stagedFile.toPath()) ||
            stagedFile.length() != newBytes || !validSignature(stagedFile)) {
            stagedFile.delete()
            throw IOException("Staged remote copy invalid")
        }
        val resolvedName = newName?.takeIf { it.endsWith(".kdbx", ignoreCase = true) } ?: old.name
        val updated = old.copy(name = resolvedName, eTag = newEtag, bytes = newBytes,
            savedAt = System.currentTimeMillis(), state = CloudCopyState.Clean, uploadAttempted = false)
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", updated.name).put("eTag", updated.eTag)
            .put("accountName", updated.accountName).put("bytes", updated.bytes)
            .put("savedAt", updated.savedAt).put("state", updated.state.name)
            .put("uploadAttempted", false)
            .put("blob", stagedBlob).toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.RemoteDownloaded, CloudCopyState.Clean, updated.bytes, updated.eTag)
        return updated
    }

    @Synchronized
    internal fun discardStagedRemoteCiphertext(id: CloudCopyId, stagedBlob: String) {
        if (!stagedBlob.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx"))) return
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) return
        val current = runCatching { read(dir)?.second }.getOrNull()
        val conflict = runCatching { conflictBlob(dir) }.getOrNull()
        if (current != stagedBlob && conflict != stagedBlob) File(dir, stagedBlob).delete()
    }

    @Synchronized
    internal fun stageConflictCiphertext(
        id: CloudCopyId,
        expectedBytes: Long,
        write: (OutputStream) -> Unit
    ): String {
        require(expectedBytes in 8..MAX_BYTES)
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        if (record.first.state != CloudCopyState.Pending && record.first.state != CloudCopyState.Conflict)
            throw CloudCopyProtectedException()
        val blob = "${UUID.randomUUID()}.kdbx"
        val file = File(dir, blob)
        try {
            file.outputStream().use { out ->
                write(out)
                out.fd.sync()
            }
            if (file.length() != expectedBytes || !validSignature(file))
                throw IOException("Cloud conflict copy verification failed")
            return blob
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    @Synchronized
    internal fun recordConflict(
        id: CloudCopyId,
        expectedBaseEtag: String,
        remoteEtag: String,
        remoteBytes: Long,
        remoteBlob: String
    ): CloudCopy {
        require(expectedBaseEtag.isNotBlank() && remoteEtag.isNotBlank() && remoteBytes in 8..MAX_BYTES &&
            remoteBlob.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")))
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val localFile = File(dir, record.second)
        val remoteFile = File(dir, remoteBlob)
        val old = record.first
        if ((old.state != CloudCopyState.Pending && old.state != CloudCopyState.Conflict) ||
            old.eTag != expectedBaseEtag || record.second == remoteBlob) {
            if (record.second != remoteBlob) remoteFile.delete()
            throw CloudCopyProtectedException()
        }
        if (!localFile.isFile || Files.isSymbolicLink(localFile.toPath()) ||
            localFile.length() != old.bytes || !validSignature(localFile)) {
            throw IOException("Local conflict copy unavailable")
        }
        if (!remoteFile.isFile || Files.isSymbolicLink(remoteFile.toPath()) ||
            remoteFile.length() != remoteBytes || !validSignature(remoteFile)) {
            remoteFile.delete()
            throw IOException("Remote conflict copy invalid")
        }
        val detectedAt = System.currentTimeMillis()
        val conflict = CloudCopyConflict(
            baseEtag = old.eTag,
            localBytes = old.bytes,
            localSavedAt = old.savedAt,
            remoteEtag = remoteEtag,
            remoteBytes = remoteBytes,
            detectedAt = detectedAt
        )
        val updated = old.copy(
            state = CloudCopyState.Conflict,
            uploadAttempted = false,
            conflict = conflict
        )
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", updated.name).put("eTag", updated.eTag)
            .put("accountName", updated.accountName).put("bytes", updated.bytes)
            .put("savedAt", updated.savedAt).put("state", updated.state.name)
            .put("uploadAttempted", false).put("blob", record.second)
            .put("conflictRemoteEtag", remoteEtag)
            .put("conflictRemoteBytes", remoteBytes)
            .put("conflictRemoteBlob", remoteBlob)
            .put("conflictDetectedAt", detectedAt)
            .toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.ConflictDetected, CloudCopyState.Conflict, remoteBytes, remoteEtag)
        return updated
    }

    @Synchronized
    internal fun resolveConflictKeepLocal(
        id: CloudCopyId,
        expectedBaseEtag: String,
        expectedRemoteEtag: String
    ): CloudCopy {
        require(expectedBaseEtag.isNotBlank() && expectedRemoteEtag.isNotBlank())
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val old = record.first
        val conflict = old.conflict ?: throw CloudCopyProtectedException()
        val remoteBlob = conflictBlob(dir) ?: throw CloudCopyProtectedException()
        if (old.state != CloudCopyState.Conflict ||
            old.eTag != expectedBaseEtag ||
            conflict.remoteEtag != expectedRemoteEtag ||
            remoteBlob == record.second) {
            throw CloudCopyProtectedException()
        }
        val localFile = File(dir, record.second)
        val remoteFile = File(dir, remoteBlob)
        if (!localFile.isFile || Files.isSymbolicLink(localFile.toPath()) ||
            localFile.length() != old.bytes || !validSignature(localFile) ||
            !remoteFile.isFile || Files.isSymbolicLink(remoteFile.toPath()) ||
            remoteFile.length() != conflict.remoteBytes || !validSignature(remoteFile)) {
            throw IOException("Conflict copies cannot be verified")
        }
        // Keep both encrypted KDBX blobs on disk (remoteFile remains as an encrypted recovery snapshot)
        // and advance the base ETag to the inspected remote ETag so the user's explicit KeepLocal choice can upload.
        val updated = old.copy(
            eTag = expectedRemoteEtag,
            state = CloudCopyState.Pending,
            uploadAttempted = false,
            conflict = null
        )
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", updated.name).put("eTag", updated.eTag)
            .put("accountName", updated.accountName).put("bytes", updated.bytes)
            .put("savedAt", updated.savedAt).put("state", updated.state.name)
            .put("uploadAttempted", false).put("blob", record.second)
            .toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.ResolvedKeepLocal, CloudCopyState.Pending, updated.bytes, updated.eTag)
        return updated
    }

    @Synchronized
    internal fun resolveConflictUseRemote(
        id: CloudCopyId,
        expectedBaseEtag: String,
        expectedRemoteEtag: String
    ): CloudCopy {
        require(expectedBaseEtag.isNotBlank() && expectedRemoteEtag.isNotBlank())
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir) ?: throw IOException("Cloud copy unavailable")
        val old = record.first
        val conflict = old.conflict ?: throw CloudCopyProtectedException()
        val remoteBlob = conflictBlob(dir) ?: throw CloudCopyProtectedException()
        if (old.state != CloudCopyState.Conflict ||
            old.eTag != expectedBaseEtag ||
            conflict.remoteEtag != expectedRemoteEtag ||
            remoteBlob == record.second) {
            throw CloudCopyProtectedException()
        }
        val localFile = File(dir, record.second)
        val remoteFile = File(dir, remoteBlob)
        if (!localFile.isFile || Files.isSymbolicLink(localFile.toPath()) ||
            localFile.length() != old.bytes || !validSignature(localFile) ||
            !remoteFile.isFile || Files.isSymbolicLink(remoteFile.toPath()) ||
            remoteFile.length() != conflict.remoteBytes || !validSignature(remoteFile)) {
            throw IOException("Conflict copies cannot be verified")
        }
        // Preserve the local encrypted KDBX blob on disk as a recovery snapshot and switch active pointer to remoteBlob.
        val updated = old.copy(
            eTag = expectedRemoteEtag,
            bytes = conflict.remoteBytes,
            savedAt = System.currentTimeMillis(),
            state = CloudCopyState.Clean,
            uploadAttempted = false,
            conflict = null
        )
        val bytes = JSONObject().put("accountId", id.accountId).put("driveId", id.driveId)
            .put("itemId", id.itemId).put("name", updated.name).put("eTag", updated.eTag)
            .put("accountName", updated.accountName).put("bytes", updated.bytes)
            .put("savedAt", updated.savedAt).put("state", updated.state.name)
            .put("uploadAttempted", false).put("blob", remoteBlob)
            .toString().toByteArray(Charsets.UTF_8)
        val metadata = AtomicFile(File(dir, "record.json"))
        val output = metadata.startWrite()
        try { output.write(bytes); metadata.finishWrite(output) }
        catch (error: Exception) { metadata.failWrite(output); throw error }
        appendLog(id, CloudSyncAction.ResolvedUseRemote, CloudCopyState.Clean, updated.bytes, updated.eTag)
        return updated
    }

    @Synchronized
    override fun recoverySnapshots(id: CloudCopyId): Int {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) return 0
        val activeBlob = runCatching { read(dir)?.second }.getOrNull() ?: return 0
        return dir.listFiles().orEmpty().count { file ->
            file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.name != activeBlob &&
                file.name.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")) &&
                runCatching { validSignature(file) }.getOrDefault(false)
        }
    }

    @Synchronized
    override fun logs(id: CloudCopyId?): List<CloudSyncLogEntry> {
        val marker = AtomicFile(File(root, "sync-log.json"))
        if (!marker.baseFile.exists() && !File(root, "sync-log.json.bak").exists()) return emptyList()
        return runCatching {
            val bytes = marker.openRead().use { it.readNBytes(262_145) }
            if (bytes.size > 262_144) return emptyList()
            val array = JSONArray(String(bytes, Charsets.UTF_8))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val entryId = CloudCopyId(
                        accountId = item.optString("accountId"),
                        driveId = item.optString("driveId"),
                        itemId = item.optString("itemId")
                    )
                    if (entryId.accountId.isBlank() || entryId.driveId.isBlank() || entryId.itemId.isBlank()) continue
                    if (id != null && entryId != id) continue
                    val action = runCatching { CloudSyncAction.valueOf(item.getString("action")) }.getOrNull() ?: continue
                    val state = runCatching { CloudCopyState.valueOf(item.getString("state")) }.getOrNull() ?: continue
                    add(
                        CloudSyncLogEntry(
                            timestamp = item.optLong("timestamp", 0L),
                            copyId = entryId,
                            action = action,
                            state = state,
                            bytes = item.optLong("bytes", 0L),
                            detail = item.optString("eTag")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    override fun remove(id: CloudCopyId) {
        val dir = directory(id)
        if (!dir.isDirectory || Files.isSymbolicLink(dir.toPath())) throw IOException("Cloud copy unavailable")
        val record = read(dir)?.first ?: throw IOException("Cloud copy state unavailable")
        val active = File(root, "active.json")
        if ((active.exists() || File(root, "active.json.bak").exists()) && selected() == null)
            throw IOException("Cloud selection state unavailable")
        if (selected()?.id == id || record.state != CloudCopyState.Clean)
            throw CloudCopyProtectedException()
        val files = dir.listFiles() ?: throw IOException("Cloud copy directory unavailable")
        if (files.isEmpty() || files.any { file ->
                !file.isFile || Files.isSymbolicLink(file.toPath()) ||
                    file.name !in setOf("record.json", "record.json.bak") &&
                    !file.name.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx"))
            }) throw IOException("Cloud copy contains unrecognized data")
        val current = read(dir) ?: throw IOException("Cloud copy unavailable")
        val blob = File(dir, current.second)
        if (blob.length() != record.bytes || !validSignature(blob))
            throw IOException("Cloud copy cannot be verified")
        // Atomic same-filesystem rename makes it disappear from the inventory as one unit.
        // No remote operation is performed; an interrupted cleanup leaves only encrypted
        // bytes in a private tombstone, not a partially deleted visible copy.
        val tombstone = File(root, ".removed-${UUID.randomUUID()}")
        Files.move(dir.toPath(), tombstone.toPath(), StandardCopyOption.ATOMIC_MOVE)
        val marker = AtomicFile(File(tombstone, "record.json"))
        marker.delete()
        files.filter { it.name.endsWith(".kdbx") }.forEach { file ->
            if (!File(tombstone, file.name).delete()) throw IOException("Copy removal incomplete")
        }
        if (!tombstone.delete()) throw IOException("Copy removal incomplete")
    }

    @Synchronized
    override fun promote(copy: CloudCopy, source: InputStream): CloudCopy {
        require(copy.state == CloudCopyState.Clean && copy.id.accountId.isNotBlank() &&
            copy.id.driveId.isNotBlank() && copy.id.itemId.isNotBlank() && copy.eTag.isNotBlank() &&
            copy.bytes in 8..MAX_BYTES && copy.name.endsWith(".kdbx", ignoreCase = true))
        val dir = directory(copy.id)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Private storage unavailable")
        val old = read(dir)
        if (old != null && old.first.state != CloudCopyState.Clean)
            throw CloudCopyProtectedException()
        // A new immutable blob is installed before committing the metadata pointer.
        // A crash can leave an orphan ciphertext blob, but never a reference to partial bytes.
        val blob = "${UUID.randomUUID()}.kdbx"
        val file = File(dir, blob)
        try {
            file.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                var written = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    written += count
                    if (written > copy.bytes) throw IOException("Cloud copy size mismatch")
                    out.write(buffer, 0, count)
                }
                if (written != copy.bytes) throw IOException("Cloud copy size mismatch")
                out.fd.sync()
            }
            if (!validSignature(file)) throw IOException("Invalid KDBX signature")
            val metadata = AtomicFile(File(dir, "record.json"))
            val record = JSONObject().put("accountId", copy.id.accountId)
                .put("driveId", copy.id.driveId).put("itemId", copy.id.itemId)
                .put("name", copy.name).put("eTag", copy.eTag)
                .put("accountName", copy.accountName)
                .put("bytes", copy.bytes).put("savedAt", copy.savedAt)
                .put("uploadAttempted", false)
                .put("state", copy.state.name).put("blob", blob).toString().toByteArray(Charsets.UTF_8)
            val output = metadata.startWrite()
            try {
                output.write(record)
                metadata.finishWrite(output)
            } catch (error: Exception) {
                metadata.failWrite(output)
                throw error
            }
            // Do not automatically delete prior blobs: a previous version may be needed for recovery.
            appendLog(copy.id, CloudSyncAction.PromoteDownload, CloudCopyState.Clean, copy.bytes, copy.eTag)
            return copy
        } catch (error: Exception) {
            file.delete() // Only this operation's unreferenced blob; never remove an existing copy.
            throw error
        }
    }

    private fun read(dir: File): Pair<CloudCopy, String>? {
        val metadata = AtomicFile(File(dir, "record.json"))
        if (!metadata.baseFile.exists() && !File(dir, "record.json.bak").exists()) return null
        val bytes = metadata.openRead().use { it.readNBytes(65_537) }
        if (bytes.size > 65_536) throw IOException("Invalid cloud copy record")
        val objectValue = JSONObject(String(bytes, Charsets.UTF_8))
        val id = CloudCopyId(objectValue.getString("accountId"), objectValue.getString("driveId"),
            objectValue.getString("itemId"))
        if (directory(id) != dir) throw IOException("Cloud copy identity mismatch")
        val blob = objectValue.getString("blob")
        if (!blob.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")) || !File(dir, blob).isFile)
            throw IOException("Cloud copy unavailable")
        val state = CloudCopyState.valueOf(objectValue.getString("state"))
        val attempted = state == CloudCopyState.Pending && objectValue.optBoolean("uploadAttempted", false)
        val baseEtag = objectValue.getString("eTag")
        val localBytes = objectValue.getLong("bytes")
        val savedAt = objectValue.getLong("savedAt")
        val conflict = if (state == CloudCopyState.Conflict) {
            val remoteEtag = objectValue.optString("conflictRemoteEtag")
            val remoteBytes = objectValue.optLong("conflictRemoteBytes", 0L)
            val remoteBlob = objectValue.optString("conflictRemoteBlob")
            val detectedAt = objectValue.optLong("conflictDetectedAt", savedAt)
            if (remoteEtag.isNotBlank() && remoteBytes in 8..MAX_BYTES &&
                remoteBlob.matches(Regex("[0-9a-fA-F-]{36}\\.kdbx")) && File(dir, remoteBlob).isFile) {
                CloudCopyConflict(
                    baseEtag = baseEtag,
                    localBytes = localBytes,
                    localSavedAt = savedAt,
                    remoteEtag = remoteEtag,
                    remoteBytes = remoteBytes,
                    detectedAt = detectedAt
                )
            } else null
        } else null
        return CloudCopy(id, objectValue.getString("name"), baseEtag,
            localBytes, savedAt,
            state, objectValue.optString("accountName"), attempted, conflict) to blob
    }

    private fun validSignature(file: File): Boolean = file.inputStream().use { input ->
        val signature = ByteArray(8)
        input.read(signature) == 8 && signature.contentEquals(byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75))
    }

    private fun attemptedBlob(dir: File, fallback: String): String {
        val bytes = AtomicFile(File(dir, "record.json")).openRead().use { it.readNBytes(65_537) }
        if (bytes.size > 65_536) throw IOException("Invalid cloud copy record")
        return JSONObject(String(bytes, Charsets.UTF_8)).optString("attemptedBlob").ifBlank { fallback }
    }

    private fun conflictBlob(dir: File): String? {
        val bytes = AtomicFile(File(dir, "record.json")).openRead().use { it.readNBytes(65_537) }
        if (bytes.size > 65_536) throw IOException("Invalid cloud copy record")
        return JSONObject(String(bytes, Charsets.UTF_8)).optString("conflictRemoteBlob").ifBlank { null }
    }

    private fun appendLog(
        id: CloudCopyId,
        action: CloudSyncAction,
        state: CloudCopyState,
        bytes: Long,
        eTag: String
    ) {
        runCatching {
            if (!root.isDirectory && !root.mkdirs()) return
            val existing = logs()
            val updated = buildList {
                add(
                    CloudSyncLogEntry(
                        timestamp = System.currentTimeMillis(),
                        copyId = id,
                        action = action,
                        state = state,
                        bytes = bytes,
                        detail = eTag
                    )
                )
                addAll(existing.take(MAX_LOG_ENTRIES - 1))
            }
            val array = JSONArray()
            updated.forEach { entry ->
                array.put(
                    JSONObject()
                        .put("accountId", entry.copyId.accountId)
                        .put("driveId", entry.copyId.driveId)
                        .put("itemId", entry.copyId.itemId)
                        .put("action", entry.action.name)
                        .put("state", entry.state.name)
                        .put("eTag", entry.detail)
                        .put("bytes", entry.bytes)
                        .put("timestamp", entry.timestamp)
                )
            }
            val payload = array.toString().toByteArray(Charsets.UTF_8)
            val marker = AtomicFile(File(root, "sync-log.json"))
            val output = marker.startWrite()
            try {
                output.write(payload)
                marker.finishWrite(output)
            } catch (error: Exception) {
                marker.failWrite(output)
            }
        }
    }

    private fun directory(id: CloudCopyId): File {
        val key = "${id.accountId}\u0000${id.driveId}\u0000${id.itemId}".toByteArray(Charsets.UTF_8)
        val name = MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
        return File(root, name)
    }

    private companion object {
        const val MAX_BYTES = 512L * 1024 * 1024
        // Graph calls this 250 MB; use decimal bytes so no binary/decimal ambiguity exceeds the limit.
        const val SMALL_UPLOAD_MAX_BYTES = 250_000_000L
        const val MAX_LOG_ENTRIES = 100
    }
}
