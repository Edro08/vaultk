package com.edro08.vaultk

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.GraphDriveBrowser
import com.edro08.vaultk.data.PrivateCloudCopyStore
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudDriveItem
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.After
import org.json.JSONObject

class GraphDriveBrowserTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testRoots = mutableListOf<File>()

    @After fun cleanSyntheticFiles() { testRoots.forEach { it.deleteRecursively() } }

    private fun store(): PrivateCloudCopyStore = PrivateCloudCopyStore(File(context.cacheDir,
        "test-browser-copies-${UUID.randomUUID()}").also { testRoots += it })

    private fun browser(open: (URL) -> HttpURLConnection) = GraphDriveBrowser(context, store(), open)

    @Test fun listsFoldersAndKdbxOnlyUsingStableIdentifiers() {
        val requested = mutableListOf<URL>()
        val browser = browser { url ->
            requested += url
            val body = if (url.path.endsWith("/root"))
                """{"id":"root-id","parentReference":{"driveId":"drive-id"}}"""
            else """{"value":[{"id":"folder-id","name":"Folder","folder":{}},
                {"id":"vault-id","name":"Vault.KDBX","file":{},"size":12,"eTag":"v1"},
                {"id":"other-id","name":"notes.txt","file":{}}]}"""
            FakeConnection(url, 200, body.toByteArray())
        }
        val items = browser.list("test-token", null)
        assertEquals(listOf("folder-id", "vault-id"), items.map { it.itemId })
        assertEquals("drive-id", items[1].driveId)
        assertTrue(requested[1].path.contains("/drives/drive-id/items/root-id/children"))
    }

    @Test fun promotesOnlyVerifiedBytesIntoDurableInventory() {
        val id = UUID.randomUUID().toString()
        val item = CloudDriveItem("test-drive", id, "sample.kdbx", false)
        val bytes = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)
        val copies = store()
        val browser = GraphDriveBrowser(context, copies) { url ->
            when {
                url.path.endsWith("/content") -> FakeConnection(url, 302, location = "https://public.dm.files.1drv.com/sample")
                url.host.endsWith("1drv.com") -> FakeConnection(url, 200, bytes)
                url.path.endsWith("/$id") -> FakeConnection(url, 200,
                    """{"id":"$id","name":"sample.kdbx","file":{},"size":12,"eTag":"v1"}""".toByteArray())
                else -> error("Unexpected request")
            }
        }
        val account = UUID.randomUUID().toString()
        val download = browser.download("test-token", account, item) { _, _ -> }
        assertEquals("v1", download.eTag)
        assertEquals(12L, download.bytes)
        val persisted = copies.list().single()
        assertEquals(CloudCopyId(account, item.driveId, item.itemId), persisted.id)
        assertEquals(CloudCopyState.Clean, persisted.state)
        assertEquals("v1", persisted.eTag)
    }

    @Test fun rejectsInsecureRedirectWithoutFetchingIt() {
        val id = UUID.randomUUID().toString()
        var externalRequested = false
        val browser = browser { url ->
            when {
                url.path.endsWith("/content") -> FakeConnection(url, 302, location = "http://example.org/evil")
                url.host == "example.org" -> { externalRequested = true; error("Insecure URL fetched") }
                else -> FakeConnection(url, 200,
                    """{"id":"$id","name":"sample.kdbx","file":{},"size":12,"eTag":"v1"}""".toByteArray())
            }
        }
        assertThrows(Exception::class.java) {
            browser.download("test-token", UUID.randomUUID().toString(),
                CloudDriveItem("test-drive", id, "sample.kdbx", false)) { _, _ -> }
        }
        assertFalse(externalRequested)
    }

    @Test fun acceptsHttpsGraphProvidedCdnWithoutBearerOrExtraRedirects() {
        val id = UUID.randomUUID().toString()
        var cdnConnection: FakeConnection? = null
        val browser = browser { url ->
            when {
                url.path.endsWith("/content") -> FakeConnection(url, 302,
                    location = "https://cdn.example.org/signed")
                url.host == "cdn.example.org" -> FakeConnection(url, 302,
                    location = "https://other.example.org/").also { cdnConnection = it }
                url.host == "other.example.org" -> error("Second redirect was followed")
                else -> FakeConnection(url, 200,
                    """{"id":"$id","name":"sample.kdbx","file":{},"size":12,"eTag":"v1"}""".toByteArray())
            }
        }
        assertThrows(Exception::class.java) {
            browser.download("test-token", UUID.randomUUID().toString(),
                CloudDriveItem("test-drive", id, "sample.kdbx", false)) { _, _ -> }
        }
        assertEquals(null, cdnConnection?.getRequestProperty("Authorization"))
        assertFalse(cdnConnection!!.instanceFollowRedirects)
    }

    @Test fun rejectsVersionChangedDuringDownloadAndNeverSendsTokenToFileHost() {
        val id = UUID.randomUUID().toString()
        var metadataCalls = 0
        var fileConnection: FakeConnection? = null
        val browser = browser { url ->
            when {
                url.path.endsWith("/content") -> FakeConnection(url, 302, location = "https://public.dm.files.1drv.com/sample")
                url.host.endsWith("1drv.com") -> FakeConnection(url, 200,
                    byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)).also { fileConnection = it }
                else -> {
                    metadataCalls++
                    FakeConnection(url, 200,
                        """{"id":"$id","name":"sample.kdbx","file":{},"size":12,"eTag":"v$metadataCalls"}""".toByteArray())
                }
            }
        }
        assertThrows(Exception::class.java) {
            browser.download("test-token", UUID.randomUUID().toString(),
                CloudDriveItem("test-drive", id, "sample.kdbx", false)) { _, _ -> }
        }
        assertEquals(2, metadataCalls)
        assertEquals(null, fileConnection?.getRequestProperty("Authorization"))
    }

    @Test fun doesNotReplacePendingCopyAfterVerifiedDownload() {
        val id = UUID.randomUUID().toString()
        val account = UUID.randomUUID().toString()
        val item = CloudDriveItem("drive", id, "test.kdbx", false)
        val bytes = byteArrayOf(3, -39, -94, -102, 103, -5, 75, -75, 1, 2, 3, 4)
        val copies = store()
        copies.promote(CloudCopy(CloudCopyId(account, "drive", id), "test.kdbx", "v1", 12,
            1L, CloudCopyState.Clean), ByteArrayInputStream(bytes))
        val record = testRoots.last().walkTopDown().first { it.name == "record.json" }
        record.writeText(JSONObject(record.readText()).put("state", "Pending").toString())
        val browser = GraphDriveBrowser(context, copies) { url ->
            when {
                url.path.endsWith("/content") -> FakeConnection(url, 302, location = "https://cdn.example.org/file")
                url.host == "cdn.example.org" -> FakeConnection(url, 200, bytes)
                else -> FakeConnection(url, 200,
                    """{"id":"$id","name":"test.kdbx","size":12,"eTag":"v2","file":{}}""".toByteArray())
            }
        }
        val failure = assertThrows(com.edro08.vaultk.domain.CloudDownloadFailure::class.java) {
            browser.download("test-token", account, item) { _, _ -> }
        }
        assertEquals(com.edro08.vaultk.domain.CloudDownloadIssue.Protected, failure.issue)
        assertEquals(CloudCopyState.Pending, copies.list().single().state)
        assertEquals("v1", copies.list().single().eTag)
    }

    private class FakeConnection(url: URL, private val code: Int, private val bytes: ByteArray = byteArrayOf(),
        private val location: String? = null) : HttpURLConnection(url) {
        override fun getResponseCode() = code
        override fun getInputStream() = ByteArrayInputStream(bytes)
        override fun getHeaderField(name: String) = if (name == "Location") location else null
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
    }
}
