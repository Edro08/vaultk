package com.edro08.vaultk.data

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic private storage and fake Graph; never uses the configured Microsoft account. */
class GraphSmallPendingUploaderTest {
    private val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "test-small-upload-${UUID.randomUUID()}")
    private val store = PrivateCloudCopyStore(root)
    private val id = CloudCopyId("account-a", "drive-id", "item-id")
    private val bytes = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)

    @After fun cleanup() { root.deleteRecursively() }

    private fun pending(copyId: CloudCopyId = id): CloudCopy {
        val copy = CloudCopy(copyId, "synthetic.kdbx", "base-v1", bytes.size.toLong(),
            1L, CloudCopyState.Clean)
        store.promote(copy, ByteArrayInputStream(bytes))
        val metadata = root.walkTopDown().first {
            it.name == "record.json" && JSONObject(it.readText()).getString("itemId") == copyId.itemId
        }
        metadata.writeText(JSONObject(metadata.readText()).put("state", "Pending").toString())
        return copy.copy(state = CloudCopyState.Pending)
    }

    @Test fun sendsOnlyExactPendingCiphertextWithBaseEtagAndNeverMarksClean() {
        pending()
        val reply = """{"id":"item-id","eTag":"new-v2","size":12,
            "parentReference":{"driveId":"drive-id"}}""".toByteArray()
        var opened: URL? = null
        val connection = FakeConnection(200, reply)
        val result = GraphSmallPendingUploader(store) { url -> opened = url; connection }
            .upload("private-token", id.accountId, id)
        assertEquals(GraphSmallPendingUploader.Outcome.AwaitingVerification("item-id", "new-v2"), result)
        assertEquals("graph.microsoft.com", opened?.host)
        assertTrue(opened.toString().endsWith("/drives/drive-id/items/item-id/content"))
        assertEquals("PUT", connection.requestMethod)
        assertEquals("base-v1", connection.getRequestProperty("If-Match"))
        assertEquals("Bearer private-token", connection.getRequestProperty("Authorization"))
        assertEquals(false, connection.instanceFollowRedirects)
        assertArrayEquals(bytes, connection.sent.toByteArray())
        assertEquals(CloudCopyState.Pending, store.list().single().state)
        assertEquals("base-v1", store.list().single().eTag)
    }

    @Test fun rejectsStaleAndMissingWithoutClearingPendingOrRetrying() {
        for ((code, expected) in listOf(412 to GraphSmallPendingUploader.Outcome.Conflict,
                404 to GraphSmallPendingUploader.Outcome.Missing,
                500 to GraphSmallPendingUploader.Outcome.Uncertain)) {
            val copyId = id.copy(itemId = "item-$code")
            pending(copyId)
            var attempts = 0
            val actual = GraphSmallPendingUploader(store) { attempts++; FakeConnection(code) }
                .upload("private-token", id.accountId, copyId)
            assertEquals(expected, actual)
            assertEquals(1, attempts)
            assertEquals(CloudCopyState.Pending, store.list().first { it.id == copyId }.state)
            assertThrows(com.edro08.vaultk.domain.CloudCopyProtectedException::class.java) {
                GraphSmallPendingUploader(PrivateCloudCopyStore(root)) { attempts++; FakeConnection(200) }
                    .upload("private-token", id.accountId, copyId)
            }
            assertEquals(1, attempts)
        }
    }

    @Test fun malformedSuccessOrNetworkLossIsUncertainNotClean() {
        pending()
        val invalid = """{"id":"other-id","eTag":"new-v2","size":12,
            "parentReference":{"driveId":"drive-id"}}""".toByteArray()
        assertEquals(GraphSmallPendingUploader.Outcome.Uncertain,
            GraphSmallPendingUploader(store) { FakeConnection(200, invalid) }
                .upload("private-token", id.accountId, id))
        val interruptedId = id.copy(itemId = "interrupted")
        pending(interruptedId)
        assertEquals(GraphSmallPendingUploader.Outcome.Uncertain,
            GraphSmallPendingUploader(store) { FakeConnection(200, failDuringWrite = true) }
                .upload("private-token", id.accountId, interruptedId))
        assertEquals(CloudCopyState.Pending, store.list().first { it.id == interruptedId }.state)
    }

    @Test fun rejectsDifferentAccountOrCleanCopyBeforeOpeningNetwork() {
        pending()
        var opened = 0
        val uploader = GraphSmallPendingUploader(store) { opened++; FakeConnection(200) }
        assertThrows(IllegalArgumentException::class.java) {
            uploader.upload("private-token", "other-account", id)
        }
        assertEquals(0, opened)
        store.promote(CloudCopy(id.copy(itemId = "clean"), "clean.kdbx", "v1", 12,
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        assertThrows(com.edro08.vaultk.domain.CloudCopyProtectedException::class.java) {
            uploader.upload("private-token", id.accountId, id.copy(itemId = "clean"))
        }
        assertEquals(0, opened)
    }

    private class FakeConnection(private val status: Int, private val reply: ByteArray = byteArrayOf(),
        private val failDuringWrite: Boolean = false) : HttpURLConnection(URL("https://graph.microsoft.com/")) {
        val sent = ByteArrayOutputStream()
        override fun getOutputStream(): ByteArrayOutputStream {
            if (failDuringWrite) throw IOException("Test-only interrupted upload")
            return sent
        }
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(reply)
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
    }
}
