package com.edro08.vaultk

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.PrivateCloudCopyStore
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudCopyProtectedException
import com.edro08.vaultk.application.ManageCloudCopies
import com.edro08.vaultk.domain.InvalidVaultKeyException
import com.edro08.vaultk.domain.VaultConflictException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import kotlinx.coroutines.runBlocking

class PrivateCloudCopyStoreTest {
    private val header = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)

    @Test fun persistsOnlyCompleteCiphertextAndSeparatesAccounts() = withStore { root, store ->
        val id = CloudCopyId("account-a", "drive", "item")
        val copy = CloudCopy(id, "test.kdbx", "etag-1", 12, 100L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(header))
        val other = copy.copy(id = id.copy(accountId = "account-b"))
        store.promote(other, ByteArrayInputStream(header))
        assertEquals(setOf(copy, other), PrivateCloudCopyStore(root).list().toSet())
        assertEquals(2, root.listFiles()!!.count { it.isDirectory })
        assertThrows(Exception::class.java) { store.promote(copy.copy(eTag = "etag-2"),
            ByteArrayInputStream(header.copyOf(9))) }
        assertEquals(setOf(copy, other), store.list().toSet())
    }

    @Test fun neverReplacesPendingCopy() = withStore { root, store ->
        val copy = CloudCopy(CloudCopyId("account", "drive", "item"), "test.kdbx",
            "etag-1", 12, 100L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(header))
        val metadata = root.walkTopDown().first { it.name == "record.json" }
        val updated = JSONObject(metadata.readText()).put("state", "Pending")
        metadata.writeText(updated.toString()) // Only a synthetic fixture in this test's private directory.
        assertThrows(Exception::class.java) {
            store.promote(copy.copy(eTag = "etag-2"), ByteArrayInputStream(header))
        }
        assertEquals(CloudCopyState.Pending, store.list().single().state)
        assertEquals("etag-1", store.list().single().eTag)
    }

    @Test fun opensSyntheticKdbxOfflineAfterRecreatingStoreAndNeverWritesIt() = withStore { root, store ->
        val bytes = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Synthetic") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("test-only".toByteArray()), output)
        }.toByteArray()
        val id = CloudCopyId("account", "drive", UUID.randomUUID().toString())
        val copy = CloudCopy(id, "test.kdbx", "etag-test", bytes.size.toLong(), 100L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(bytes))
        store.select(id)
        val persistedFile = root.walkTopDown().first { it.extension == "kdbx" }
        val original = persistedFile.readBytes()
        val offline = ManageCloudCopies(PrivateCloudCopyStore(root))
        assertEquals(copy, offline.selected())
        runBlocking {
            assertEquals("Synthetic", offline.open(id, "test-only".toCharArray()).root.entries.single().title)
            assertThrows(InvalidVaultKeyException::class.java) {
                runBlocking { offline.open(id, "wrong".toCharArray()) }
            }
        }
        assertEquals(true, original.contentEquals(persistedFile.readBytes()))
    }

    @Test fun removesOnlyUnselectedCleanCopyAndKeepsAnotherAccount() = withStore { root, store ->
        val first = CloudCopy(CloudCopyId("account-a", "drive", "item"), "test.kdbx",
            "v1", 12, 100L, CloudCopyState.Clean)
        val second = first.copy(id = first.id.copy(accountId = "account-b"))
        store.promote(first, ByteArrayInputStream(header))
        store.promote(second, ByteArrayInputStream(header))
        store.select(first.id)
        assertThrows(CloudCopyProtectedException::class.java) { store.remove(first.id) }
        store.select(null)
        store.remove(first.id)
        assertEquals(listOf(second), PrivateCloudCopyStore(root).list())
        assertEquals(null, store.selected())
    }

    @Test fun neverRemovesPendingOrUnverifiableCopy() = withStore { root, store ->
        val copy = CloudCopy(CloudCopyId("account", "drive", "item"), "test.kdbx",
            "v1", 12, 100L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(header))
        val metadata = root.walkTopDown().first { it.name == "record.json" }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Pending").toString())
        assertThrows(CloudCopyProtectedException::class.java) { store.remove(copy.id) }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Clean").toString())
        val unknown = File(metadata.parentFile, "unknown.part")
        unknown.writeText("not a known snapshot")
        assertThrows(Exception::class.java) { store.remove(copy.id) }
        assertEquals(copy, store.list().single())
    }

    @Test fun verifiedLocalEditBecomesDurablePendingWithoutChangingBaseEtag() = withStore { root, store ->
        val bytes = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Before") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("test-only".toByteArray()), output)
        }.toByteArray()
        val copy = CloudCopy(CloudCopyId("account", "drive", UUID.randomUUID().toString()),
            "synthetic.kdbx", "base-etag", bytes.size.toLong(), 1L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(bytes))
        val opened = store.openVersioned(copy.id, "test-only".toCharArray())
        val updated = opened.content.copy(root = opened.content.root.copy(entries =
            opened.content.root.entries.map { it.copy(title = "After") }))
        val saved = store.savePending(copy.id, opened.fingerprint, "test-only".toCharArray(),
            opened.content, updated)
        val restarted = PrivateCloudCopyStore(root)
        assertEquals(CloudCopyState.Pending, restarted.list().single().state)
        assertEquals("base-etag", restarted.list().single().eTag)
        assertEquals("After", restarted.open(copy.id, "test-only".toCharArray()).root.entries.single().title)
        assertEquals("After", saved.content.root.entries.single().title)
        assertEquals(2, root.walkTopDown().count { it.extension == "kdbx" })
        assertThrows(VaultConflictException::class.java) {
            store.savePending(copy.id, opened.fingerprint, "test-only".toCharArray(),
                opened.content, updated)
        }
        assertThrows(CloudCopyProtectedException::class.java) { store.remove(copy.id) }
        assertThrows(CloudCopyProtectedException::class.java) {
            store.promote(copy, ByteArrayInputStream(bytes))
        }
    }

    @Test fun wrongKeyCannotPublishPendingOrReplaceCleanCopy() = withStore { root, store ->
        val bytes = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Before") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("test-only".toByteArray()), output)
        }.toByteArray()
        val copy = CloudCopy(CloudCopyId("account", "drive", "item"), "synthetic.kdbx",
            "base-etag", bytes.size.toLong(), 1L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(bytes))
        val opened = store.openVersioned(copy.id, "test-only".toCharArray())
        assertThrows(InvalidVaultKeyException::class.java) {
            store.savePending(copy.id, opened.fingerprint, "wrong".toCharArray(),
                opened.content, opened.content)
        }
        assertEquals(listOf(copy), PrivateCloudCopyStore(root).list())
        assertEquals(1, root.walkTopDown().count { it.extension == "kdbx" })
    }

    @Test fun editingAttemptedPendingCopyCannotRearmBlindUpload() = withStore { root, store ->
        val bytes = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Before") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials("test-only".toByteArray()), output)
        }.toByteArray()
        val id = CloudCopyId("account", "drive", UUID.randomUUID().toString())
        store.promote(CloudCopy(id, "synthetic.kdbx", "base-etag", bytes.size.toLong(),
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        val opened = store.openVersioned(id, "test-only".toCharArray())
        val first = store.savePending(id, opened.fingerprint, "test-only".toCharArray(),
            opened.content, opened.content)
        store.claimSmallPendingUpload(id)
        store.savePending(id, first.fingerprint, "test-only".toCharArray(),
            first.content, first.content)
        assertEquals(CloudCopyState.Pending, PrivateCloudCopyStore(root).list().single().state)
        val record = root.walkTopDown().first { it.name == "record.json" }
        assertEquals(true, JSONObject(record.readText()).getBoolean("uploadAttempted"))
        assertThrows(CloudCopyProtectedException::class.java) {
            PrivateCloudCopyStore(root).claimSmallPendingUpload(id)
        }
    }

    private fun withStore(block: (File, PrivateCloudCopyStore) -> Unit) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "test-cloud-copies-${UUID.randomUUID()}")
        try { block(root, PrivateCloudCopyStore(root)) }
        finally { root.deleteRecursively() } // This test's randomly created directory only.
    }
}
