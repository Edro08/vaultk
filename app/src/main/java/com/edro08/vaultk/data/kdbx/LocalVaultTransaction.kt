package com.edro08.vaultk.data.kdbx
import com.edro08.vaultk.domain.vault.VaultConflictException

import com.edro08.vaultk.domain.vault.VaultSaveProgress
import com.edro08.vaultk.domain.vault.VaultSaveStage
import com.edro08.vaultk.domain.vault.VaultContent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Only replaces a local vault after its encrypted replacement has been durably written and opened. */
class LocalVaultTransaction(private val reader: KdbxReader = KdbxReader()) {
    fun fingerprint(file: File): ByteArray = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input ->
            val block = ByteArray(8192)
            while (true) {
                val size = input.read(block)
                if (size < 0) break
                digest.update(block, 0, size)
            }
        }
        digest.digest()
    }

    /** A failed preparation leaves the original untouched; a failed commit retains the backup. */
    fun commit(file: File, expected: ByteArray, replacement: ByteArray, key: CharArray,
        onTiming: (String, Long) -> Unit = { _, _ -> },
        onProgress: (VaultSaveProgress) -> Unit = {}): Pair<VaultContent, ByteArray> {
        val directory = file.canonicalFile.parentFile ?: throw IOException("No vault directory")
        val original = file.canonicalFile
        if (!original.isFile || !MessageDigest.isEqual(expected, fingerprint(original)))
            throw VaultConflictException()
        val staged = File.createTempFile(".vaultk-stage-", ".tmp", directory)
        var backup: File? = null
        var moved = false
        try {
            onProgress(VaultSaveProgress(VaultSaveStage.Writing, 0f))
            FileOutputStream(staged).use { output ->
                var written = 0
                val chunkSize = maxOf(64 * 1024, (replacement.size + 99) / 100)
                while (written < replacement.size) {
                    val count = minOf(chunkSize, replacement.size - written)
                    output.write(replacement, written, count)
                    written += count
                    onProgress(VaultSaveProgress(VaultSaveStage.Writing,
                        written.toFloat() / replacement.size))
                }
                output.fd.sync()
            }
            onProgress(VaultSaveProgress(VaultSaveStage.Verifying))
            val verifyStarted = System.nanoTime()
            if (!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(replacement), fingerprint(staged)))
                throw IOException("Staged vault differs")
            onTiming("hashTemporary", (System.nanoTime() - verifyStarted) / 1_000_000)
            val openStarted = System.nanoTime()
            val verified = staged.inputStream().use { reader.read(it, key) }
            onTiming("decryptTemporary", (System.nanoTime() - openStarted) / 1_000_000)
            if (!MessageDigest.isEqual(expected, fingerprint(original))) throw VaultConflictException()
            onProgress(VaultSaveProgress(VaultSaveStage.Replacing))
            // Keep a verified original for recovery if the process dies just after the atomic move.
            backup = File.createTempFile(".vaultk-backup-", ".tmp", directory)
            FileOutputStream(backup).use { output ->
                original.inputStream().use { it.copyTo(output) }
                output.fd.sync()
            }
            if (!MessageDigest.isEqual(expected, fingerprint(backup))) throw VaultConflictException()
            if (!MessageDigest.isEqual(expected, fingerprint(original))) throw VaultConflictException()
            try {
                Files.move(staged.toPath(), original.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING)
            } catch (error: AtomicMoveNotSupportedException) {
                throw IOException("Atomic replacement is unavailable", error)
            }
            moved = true
            val actual = fingerprint(original)
            if (!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(replacement), actual))
                throw IOException("Committed vault differs; backup retained")
            // The committed file has exactly the bytes decrypted above; a second KDBX
            // open here would run the same expensive KDF without adding verification.
            backup.delete()
            return verified to actual
        } finally {
            staged.delete()
            if (!moved) backup?.delete()
        }
    }
}
