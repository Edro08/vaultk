package com.edro08.vaultk

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.application.OpenLocalVault
import com.edro08.vaultk.application.SaveLocalVault
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.domain.VaultId
import com.edro08.vaultk.data.KdbxReader
import com.edro08.vaultk.data.KdbxWriter
import com.edro08.vaultk.data.LocalVaultFiles
import com.edro08.vaultk.data.LocalKdbxVaultRepository
import com.edro08.vaultk.data.LocalVaultTransaction
import com.edro08.vaultk.domain.VaultConflictException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.File

class LocalKdbxVaultRepositoryTest {
    @Test fun openSaveReopenAndRejectStaleFingerprint() = runBlocking {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "gateway-${System.nanoTime()}").apply { mkdirs() }
        val file = File(directory, "isolated.kdbx")
        val key = "test-only".toCharArray()
        try {
            KdbxDatabase().apply { rootGroup.addEntry("Before") }.let { database ->
                file.outputStream().use { database.save(KdbxStreamFormat(KdbxHeader(4)),
                    KdbxCredentials("test-only".toByteArray()), it) }
            }
            val reader = KdbxReader()
            val gateway = LocalKdbxVaultRepository(
                LocalVaultFiles(directory) { true }, reader, KdbxWriter(reader),
                LocalVaultTransaction(reader), attachmentInput = { error("No attachment") })
            val opened = OpenLocalVault(gateway)(VaultId(file.path), key)
            val changed = opened.content.copy(root = opened.content.root.copy(entries =
                opened.content.root.entries.map { it.copy(title = "After") }))
            val stages = mutableListOf<VaultSaveStage>()
            val saved = SaveLocalVault(gateway)(VaultId(file.path), opened.fingerprint, key,
                opened.content, changed) { stages += it.stage }
            assertEquals(listOf(VaultSaveStage.Preparing, VaultSaveStage.Writing,
                VaultSaveStage.Verifying, VaultSaveStage.Replacing), stages.distinct())
            assertEquals("After", OpenLocalVault(gateway)(VaultId(file.path), key)
                .content.root.entries.single().title)
            assertThrows(VaultConflictException::class.java) {
                gateway.save(VaultId(file.path), opened.fingerprint, key, opened.content, changed) {}
            }
            assertArrayEquals(saved.fingerprint, LocalVaultTransaction(reader).fingerprint(file))
        } finally {
            key.fill('\u0000')
            directory.deleteRecursively()
        }
    }
}
