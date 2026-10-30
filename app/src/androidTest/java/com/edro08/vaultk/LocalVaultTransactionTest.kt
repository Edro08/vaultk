package com.edro08.vaultk

import androidx.test.platform.app.InstrumentationRegistry
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import com.edro08.vaultk.data.kdbx.KdbxReader
import com.edro08.vaultk.data.kdbx.KdbxWriter
import com.edro08.vaultk.data.kdbx.LocalVaultTransaction
import com.edro08.vaultk.domain.vault.VaultConflictException
import com.edro08.vaultk.domain.vault.VaultSaveProgress
import com.edro08.vaultk.domain.vault.VaultSaveStage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.File

class LocalVaultTransactionTest {
    @Test fun commitsFreshVaultInSharedStorage() {
        if (!Environment.isExternalStorageManager()) return
        val directory = File(Environment.getExternalStorageDirectory(),
            "Documents/Vaultk-transaction-test-${System.nanoTime()}").apply { mkdirs() }
        val file = File(directory, "new-test.kdbx")
        val key = "maestra".toCharArray()
        try {
            val database = KdbxDatabase()
            database.rootGroup.addGroup("Prueba").addEntry("Antes")
            file.outputStream().use { database.save(KdbxStreamFormat(KdbxHeader(4)),
                KdbxCredentials("maestra".toByteArray()), it) }
            val reader = KdbxReader()
            val original = file.inputStream().use { reader.read(it, key) }
            val group = original.root.groups.single()
            val changed = original.copy(root = original.root.copy(groups = listOf(group.copy(
                entries = group.entries.map { it.copy(title = "Después") }))))
            val bytes = file.inputStream().use { KdbxWriter(reader).prepare(it, key, original, changed) {
                error("No attachment")
            } }
            val transaction = LocalVaultTransaction(reader)
            transaction.commit(file, transaction.fingerprint(file), bytes, key)
            assertEquals("Después", file.inputStream().use { reader.read(it, key) }
                .root.groups.single().entries.single().title)
        } finally {
            key.fill('\u0000')
            directory.deleteRecursively()
        }
    }

    @Test fun replacementIsReadableAndConflictsNeverOverwriteOriginal() {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "transaction-${System.nanoTime()}").apply { mkdirs() }
        val file = File(directory, "fresh-test.kdbx")
        val key = "maestra".toCharArray()
        try {
            val database = KdbxDatabase()
            database.rootGroup.addGroup("Cuentas").addEntry("Anterior")
            file.outputStream().use { database.save(KdbxStreamFormat(KdbxHeader(4)),
                KdbxCredentials("maestra".toByteArray()), it) }
            val reader = KdbxReader()
            val transaction = LocalVaultTransaction(reader)
            val original = file.inputStream().use { reader.read(it, key) }
            val changed = original.copy(root = original.root.copy(groups = original.root.groups.map { group ->
                if (group.name == "Cuentas") group.copy(entries = group.entries.map { it.copy(title = "Nuevo") })
                else group
            }))
            val prepareStarted = SystemClock.elapsedRealtime()
            val replacement = file.inputStream().use { input ->
                KdbxWriter(reader).prepare(input, key, original, changed) { error("No attachment") }
            }
            val preparedAt = SystemClock.elapsedRealtime()
            val before = file.readBytes()
            val hash = transaction.fingerprint(file)
            assertThrows(VaultConflictException::class.java) {
                transaction.commit(file, ByteArray(hash.size), replacement, key)
            }
            assertArrayEquals(before, file.readBytes())
            val invalidStages = mutableListOf<VaultSaveProgress>()
            assertThrows(Exception::class.java) {
                transaction.commit(file, hash, byteArrayOf(1, 2, 3), key, onProgress = invalidStages::add)
            }
            assertTrue(invalidStages.none { it.stage == VaultSaveStage.Replacing })
            assertArrayEquals(before, file.readBytes())
            val commitStarted = SystemClock.elapsedRealtime()
            val stages = mutableListOf<VaultSaveProgress>()
            val stageTimes = mutableMapOf<VaultSaveStage, Long>()
            val (saved, newHash) = transaction.commit(file, hash, replacement, key) { update ->
                stages.add(update)
                stageTimes.putIfAbsent(update.stage, SystemClock.elapsedRealtime())
            }
            assertEquals(listOf(VaultSaveStage.Writing, VaultSaveStage.Verifying, VaultSaveStage.Replacing),
                stages.map { it.stage }.distinct())
            assertEquals(0f, stages.first().writtenFraction)
            assertEquals(1f, stages.last { it.stage == VaultSaveStage.Writing }.writtenFraction)
            Log.i("VaultkSaveTiming", "Synthetic KDBX: prepare=${preparedAt - prepareStarted}ms, " +
                "commit=${SystemClock.elapsedRealtime() - commitStarted}ms, " +
                "write=${stageTimes.getValue(VaultSaveStage.Verifying) - stageTimes.getValue(VaultSaveStage.Writing)}ms, " +
                "verify=${stageTimes.getValue(VaultSaveStage.Replacing) - stageTimes.getValue(VaultSaveStage.Verifying)}ms")
            assertTrue(directory.listFiles()!!.contentEquals(arrayOf(file)))
            assertEquals("Nuevo", saved.root.groups.single().entries.single().title)
            assertArrayEquals(newHash, transaction.fingerprint(file))
            assertEquals("Nuevo", file.inputStream().use { reader.read(it, key) }
                .root.groups.single().entries.single().title)
            assertThrows(VaultConflictException::class.java) {
                transaction.commit(file, hash, replacement, key)
            }
            assertArrayEquals(newHash, transaction.fingerprint(file))
        } finally {
            key.fill('\u0000')
            directory.deleteRecursively()
        }
    }
}
