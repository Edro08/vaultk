package com.edro08.vaultk.data
import com.edro08.vaultk.domain.VaultConflictException

import android.net.Uri
import com.edro08.vaultk.domain.VaultRepository
import com.edro08.vaultk.domain.VaultId
import com.edro08.vaultk.domain.OpenedVault
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultContent
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Keeps file access, KDBX serialization and the verified atomic commit in infrastructure. */
class LocalKdbxVaultRepository(
    private val files: LocalVaultFiles,
    private val reader: KdbxReader,
    private val writer: KdbxWriter,
    private val transaction: LocalVaultTransaction,
    private val attachmentInput: (Uri) -> InputStream,
    private val onTiming: (String, Long) -> Unit = { _, _ -> },
) : VaultRepository {
    override fun open(id: VaultId, key: CharArray): OpenedVault {
        val file = File(id.value)
        if (!files.canRead(Uri.fromFile(file))) throw SecurityException("Local vault is unavailable")
        val before = transaction.fingerprint(file)
        val content = files.open(Uri.fromFile(file)).use { reader.read(it, key) }
        val after = transaction.fingerprint(file)
        if (!MessageDigest.isEqual(before, after)) throw VaultConflictException()
        return OpenedVault(content, after)
    }

    override fun save(id: VaultId, expectedFingerprint: ByteArray, key: CharArray,
        original: VaultContent, changed: VaultContent,
        onProgress: (VaultSaveProgress) -> Unit): OpenedVault {
        val file = File(id.value)
        if (!files.canRead(Uri.fromFile(file))) throw SecurityException("Local vault is unavailable")
        if (!MessageDigest.isEqual(expectedFingerprint, transaction.fingerprint(file)))
            throw VaultConflictException()
        var prepared: ByteArray? = null
        try {
            val replacement = file.inputStream().use { input ->
                writer.prepare(input, key, original, changed, onTiming, attachmentInput)
            }
            prepared = replacement
            val (content, fingerprint) = transaction.commit(file, expectedFingerprint,
                replacement, key, onTiming, onProgress)
            return OpenedVault(content, fingerprint)
        } finally {
            prepared?.fill(0)
        }
    }
}
